/* Name: hidtool.c
 * Project: hid-data example
 * Author: Christian Starkjohann
 * Creation Date: 2008-04-11
 * Tabsize: 4
 * Copyright: (c) 2008 by OBJECTIVE DEVELOPMENT Software GmbH
 * License: GNU GPL v2 (see License.txt), GNU GPL v3 or proprietary (CommercialLicense.txt)
 */
#include <sys/time.h>

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "hiddata.h"
#include "usbconfig.h"  /* for device VID, PID, vendor name and product name */
/*
#define  USB_CFG_VENDOR_ID       0x42, 0x42
/* USB vendor ID for the device, low byte first. If you have registered your
 * own Vendor ID, define it here. Otherwise you use one of obdev's free shared
 * VID/PID pairs. Be sure to read USB-IDs-for-free.txt for rules!
 *//*
#define  USB_CFG_DEVICE_ID       0x00, 0x02
/* This is the ID of the product, low byte first. It is interpreted in the
 * scope of the vendor ID. If you have registered your own VID with usb.org
 * or if you have licensed a PID from somebody else, define it here. Otherwise
 * you use obdev's free shared VID/PID pair. Be sure to read the rules in
 * USB-IDs-for-free.txt!
 */
/* ------------------------------------------------------------------------- */

#define bytes_to_u16(MSB,LSB) (((unsigned int) ((unsigned char) MSB)) & 255)<<8 | (((unsigned char) LSB)&255)

static char *usbErrorMessage(int errCode)
{
static char buffer[40];

    switch(errCode){
        case USBOPEN_ERR_ACCESS:      return "Access to device denied";
        case USBOPEN_ERR_NOTFOUND:    return "The specified device was not found";
        case USBOPEN_ERR_IO:          return "Communication error with device";
        default:
            sprintf(buffer, "Unknown USB error %d", errCode);
            return buffer;
    }
    return NULL;    /* not reached */
}

static usbDevice_t  *openDevice(void)
{
usbDevice_t     *dev = NULL;
unsigned char   rawVid[2] = {USB_CFG_VENDOR_ID}, rawPid[2] = {USB_CFG_DEVICE_ID};
char            vendorName[] = {USB_CFG_VENDOR_NAME, 0}, productName[] = {USB_CFG_DEVICE_NAME, 0};
int             vid = rawVid[0] + 256 * rawVid[1];
int             pid = rawPid[0] + 256 * rawPid[1];
int             err;

    if((err = usbhidOpenDevice(&dev, vid, vendorName, pid, productName, 0)) != 0){
        fprintf(stderr, "error finding %s: %s\n", productName, usbErrorMessage(err));
        return NULL;
    }
    return dev;
}

/* ------------------------------------------------------------------------- */
static void valuesdump(char *buffer, int len)
{
int     i;
FILE    *fp = stdout;

    for(i = 0; i < len; i++){
        
        unsigned int portValue = bytes_to_u16(buffer[i+1],buffer[i]);
        fprintf(fp, "%u \n", portValue);
        //fprintf(fp, "\n");
        i++;
        /*
        if(i != 0){
            if(i % 2 == 0){
                fprintf(fp, "\n");
            }else{
                fprintf(fp, " ");
            }
        }
        char outValue = buffer[i] & 0xff;
        fprintf(fp, "0x%02x", outValue);
        */
    }
    if(i != 0)
        fprintf(fp, "\n");    
}


static void hexdump(char *buffer, int len)
{
int     i;
FILE    *fp = stdout;

    for(i = 0; i < len; i++){
        if(i != 0){
            if(i % 16 == 0){
                fprintf(fp, "\n");
            }else{
                fprintf(fp, " ");
            }
        }
        fprintf(fp, "0x%02x", buffer[i] & 0xff);
    }
    if(i != 0)
        fprintf(fp, "\n");
}

static int  hexread(char *buffer, char *string, int buflen)
{
char    *s;
int     pos = 0;

    while((s = strtok(string, ", ")) != NULL && pos < buflen){
        string = NULL;
        buffer[pos++] = (char)strtol(s, NULL, 0);
    }
    return pos;
}

/* ------------------------------------------------------------------------- */

static void usage(char *myName)
{
    fprintf(stderr, "usage:\n");
    fprintf(stderr, "  %s read\n", myName);
    fprintf(stderr, "  %s write <listofbytes>\n", myName);
}
/*
long long timeInMilliseconds(void){
	struct timeval tv;
	gettimeofday(&tv,NULL);
	return (((long long) tv.tv_sec)*1000)+(tv.tv_usec/1000);
	}
*/
int main(int argc, char **argv)
{
usbDevice_t *dev;
char        buffer[129];    /* room for dummy report ID */
int         err;

struct timeval stop, start;
gettimeofday(&start, NULL);

    if(argc < 2){
        usage(argv[0]);
        exit(1);
    }
    if((dev = openDevice()) == NULL)
        exit(1);
    if(strcasecmp(argv[1], "read") == 0){
        int len = sizeof(buffer);
        gettimeofday(&start, NULL);
	while (dev != NULL){

        	if((err = usbhidGetReport(dev, 0, buffer, &len)) != 0){
            		fprintf(stderr, "error reading data: %s\n", usbErrorMessage(err));
        	}else{
	    		gettimeofday(&stop, NULL);
			//printf("Time %llu ms\n", timeInMilliseconds());
	    		printf("%lu \n", (stop.tv_sec - start.tv_sec)*1000000 + (stop.tv_usec - start.tv_usec));
			gettimeofday(&start, NULL);
			//printf("Hex value: %x\n", buffer);
	    		//printf("\n");
            		//hexdump(buffer + 1, sizeof(buffer) - 1);
            		//valuesdump(buffer + 1, sizeof(buffer) - 1);
        	}
	}
    }else if(strcasecmp(argv[1], "write") == 0){
        int i, pos;
        memset(buffer, 0, sizeof(buffer));
        for(pos = 1, i = 2; i < argc && pos < sizeof(buffer); i++){
            pos += hexread(buffer + pos, argv[i], sizeof(buffer) - pos);
        }
        if((err = usbhidSetReport(dev, buffer, sizeof(buffer))) != 0)   /* add a dummy report ID */
            fprintf(stderr, "error writing data: %s\n", usbErrorMessage(err));
    }else{
        usage(argv[0]);
        exit(1);
    }
    usbhidCloseDevice(dev);
    return 0;
}

/* ------------------------------------------------------------------------- */
