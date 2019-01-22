/* Robot Code
 by: Tyrone Verburgt.
 Code used to interface a Bluetooth Adapter, Servo motor, the BMP085
 temperature and pressure sensor and a JPEG camera the the ATMega328-PU
 microcontroller. This code addtionally provides code to use an H-bridge
 that can contro the polarity of motors.
 Date: 03 June 2015
*/

#include <SoftwareSerial.h> //Library used to create a Software Serial Port.
#include <Wire.h>  //Library used to handle I2C communicatio interface.
#include <Servo.h> //Library used operate a servo motor.
#define BMP085_ADDRESS 0x77  // I2C address of BMP085.
byte incomingbyte;
SoftwareSerial mySerial = SoftwareSerial(13, 4); //SoftwareSerial(rx of arduino, tx of arduino).
int a=0x0000,j=0,k=0,count=0; //Read Starting address .      
uint8_t MH,ML;
//Global variable flag use to indicate that all image data of a taken picture is received.
boolean EndFlag=0;  
int x = 0;
int z = 0;
Servo myservo;  // create servo object to control a servo.
byte servoAnglePosition = 0;


//*********Initialised camera functions for the camera*************//    
void SendResetCmd();
void SendTakePhotoCmd();
void SendReadDataCmd();
void StopTakePhotoCmd();
//*****************************************************************// 


//******Calibration variables parameters for BMP085 Sensor*********//
short temperature;
long pressure;
float altitude;
const unsigned char OSS = 0;  // Oversampling Setting.
int ac1;
int ac2; 
int ac3; 
unsigned int ac4;
unsigned int ac5;
unsigned int ac6;
int b1; 
int b2;
int mb;
int mc;
int md;
// b5 is calculated in bmp085GetTemperature(...), this variable is also used in bmp085GetPressure(...)
// so ...Temperature(...) must be called before ...Pressure(...).
long b5; 
// Use these for altitude conversions
const float p0 = 101325;     // Pressure at sea level (Pa)
//**************************************************************//



void setup() //Called only once at the start of the program.
{ 
  Serial.begin(9600);  //Don't forget to change this as the baud rate between the computer and ATMEL chip is 9600
  mySerial.begin(38400);
  Wire.begin();
  bmp085Calibration();
  myservo.attach(6);  //Attaches the servo on pin 6 (PWM).
  
  pinMode(7, OUTPUT);
  pinMode(8, OUTPUT);
  pinMode(9, OUTPUT);
  pinMode(10, OUTPUT);
  pinMode(12, OUTPUT);
  

  //Keep robot at stand still.
  digitalWrite(7, HIGH);
  digitalWrite(10, HIGH);
  
  digitalWrite(8, HIGH);
  digitalWrite(9, HIGH);
  //**************************
  
  
  
}

void loop() //A loop that loops to infinity.
{
  
 if (Serial.available() > 0) {   
  byte input = Serial.read();
  //Remember that the Serial monitor of this software returns values in ASCII. For Serial.read() == 97 to be true,
  //the letter a must be entered in the Serial monitor as the ASCII number for a is 97.  On the other hand
  //the bluetooth adapter HC-05 returns the actual number 100. 
  if(input == 'p')  //A value of 100 received indicates that a picture must be taken and send back.
  {     
    SendResetCmd(); 
    delay(4000);
    SendTakePhotoCmd();


     while(mySerial.available()>0)
      {
        incomingbyte=mySerial.read();
      }   
      byte a[32];
      
      while(!EndFlag)  //While not true [!Endflag = false] where Endflag = 0
      {  
         j=0;
         k=0;
         count=0;
         SendReadDataCmd();

         delay(400); //Play with this value
          while(mySerial.available()>0)
          {
               incomingbyte=mySerial.read();
               k++;
               if((k>5)&&(j<32)&&(!EndFlag))
               {
               a[j]=incomingbyte;
               if((a[j-1]==0xFF)&&(a[j]==0xD9))      //Check if the picture is over
               {
                 EndFlag=1;
               }
                                          
               j++;
	       count++;
               }
          }
         
         //Send jpeg picture over the serial port
          for(j=0;j<count;j++)
          {   
              if(a[j]<0x10)            //If element value is less than 16 (0x10). 16 is the highest number for a byte.
              Serial.print("0");
              Serial.print(a[j],HEX);
              
              if((a[j-1]==0xFF)&&(a[j]==0xD9))  //Check if the picture is over
              {
               Serial.print("#"); //Sent to android application to indicate end of picture.
              } 
          }                          
      }        //End of while loop.
    }         //End of if statement.
     EndFlag = 0;
           
          
          if(input == 'd')
          {
            
             temperature = bmp085GetTemperature(bmp085ReadUT());
             pressure = bmp085GetPressure(bmp085ReadUP());
             altitude = getAltitude(pressure);


             Serial.print('*');
             Serial.print( (analogRead(A3)/1023 ) * 100);
             
             Serial.print('*');
             Serial.print(temperature*0.1);
             
             Serial.print('*');
             Serial.print(pressure);
             
             Serial.print('*');
             Serial.print(altitude);
             Serial.print('*');
             

             digitalWrite(12, HIGH);
          }
          
          
          if(input == 'f')
          {
             forward(); 
          }
          
          if(input == 'b')
          {
             halt(); 
          }
          
          if(input == 'l')
          {
             left(); 
          }
          if(input == 'r')
          {
             right(); 
          }      
         

      if(input >= 1 && input <= 180 && input != 'd')  //'r', 'l', 'f', 'b', 'd', 'p
      {
          myservo.write(input);
      }
  } 
}


//Send Reset command
void SendResetCmd() {
  mySerial.write((byte)0x56);
  mySerial.write((byte)0x00);
  mySerial.write((byte)0x26);
  mySerial.write((byte)0x00);   
}

//Send take picture command
void SendTakePhotoCmd() {
  mySerial.write((byte)0x56);
  mySerial.write((byte)0x00);
  mySerial.write((byte)0x36);
  mySerial.write((byte)0x01);
  mySerial.write((byte)0x00);
    
  a = 0x0000; //reset so that another picture can taken
}

void FrameSize() {
  mySerial.write((byte)0x56);
  mySerial.write((byte)0x00);
  mySerial.write((byte)0x34);
  mySerial.write((byte)0x01);
  mySerial.write((byte)0x00);  
}

//Read data
void SendReadDataCmd() {
  MH=a/0x100;
  ML=a%0x100;
      
  mySerial.write((byte)0x56);
  mySerial.write((byte)0x00);
  mySerial.write((byte)0x32);
  mySerial.write((byte)0x0c);
  mySerial.write((byte)0x00);
  mySerial.write((byte)0x0a);
  mySerial.write((byte)0x00);
  mySerial.write((byte)0x00);
  mySerial.write((byte)MH);
  mySerial.write((byte)ML);
  mySerial.write((byte)0x00);
  mySerial.write((byte)0x00);
  mySerial.write((byte)0x00);
  mySerial.write((byte)0x20);
  mySerial.write((byte)0x00);
  mySerial.write((byte)0x0a);

  a+=0x20; 
}

void StopTakePhotoCmd() {
  mySerial.write((byte)0x56);
  mySerial.write((byte)0x00);
  mySerial.write((byte)0x36);
  mySerial.write((byte)0x01);
  mySerial.write((byte)0x03);        
}


void bmp085Calibration()
{
  ac1 = bmp085ReadInt(0xAA);
  ac2 = bmp085ReadInt(0xAC);
  ac3 = bmp085ReadInt(0xAE);
  ac4 = bmp085ReadInt(0xB0);
  ac5 = bmp085ReadInt(0xB2);
  ac6 = bmp085ReadInt(0xB4);
  b1 = bmp085ReadInt(0xB6);
  b2 = bmp085ReadInt(0xB8);
  mb = bmp085ReadInt(0xBA);
  mc = bmp085ReadInt(0xBC);
  md = bmp085ReadInt(0xBE);
}




// Read 1 byte from the BMP085 at 'address'
char bmp085Read(unsigned char address)
{
  unsigned char data;
  
  Wire.beginTransmission(BMP085_ADDRESS);
  Wire.write(address);
  Wire.endTransmission();
  
  Wire.requestFrom(BMP085_ADDRESS, 1);
  while(!Wire.available())
    ;
    
  return Wire.read();
}

// Read 2 bytes from the BMP085
// First byte will be from 'address'
// Second byte will be from 'address'+1
int bmp085ReadInt(unsigned char address)
{
  unsigned char msb, lsb;
  
  Wire.beginTransmission(BMP085_ADDRESS);
  Wire.write(address);
  Wire.endTransmission();
  
  Wire.requestFrom(BMP085_ADDRESS, 2);
  while(Wire.available()<2)
    ;
  msb = Wire.read();
  lsb = Wire.read();
  
  return (int) msb<<8 | lsb;
}



// Read the uncompensated temperature value
unsigned int bmp085ReadUT()
{
  unsigned int ut;
  
  // Write 0x2E into Register 0xF4
  // This requests a temperature reading
  Wire.beginTransmission(BMP085_ADDRESS);
  Wire.write(0xF4);
  Wire.write(0x2E);
  Wire.endTransmission();
  
  // Wait at least 4.5ms
  delay(5);
  
  // Read two bytes from registers 0xF6 and 0xF7
  ut = bmp085ReadInt(0xF6);
  return ut;
}

// Read the uncompensated pressure value
unsigned long bmp085ReadUP()
{
  unsigned char msb, lsb, xlsb;
  unsigned long up = 0;
  
  // Write 0x34+(OSS<<6) into register 0xF4
  // Request a pressure reading w/ oversampling setting
  Wire.beginTransmission(BMP085_ADDRESS);
  Wire.write(0xF4);
  Wire.write(0x34 + (OSS<<6));
  Wire.endTransmission();
  
  // Wait for conversion, delay time dependent on OSS
  delay(2 + (3<<OSS));
  
  // Read register 0xF6 (MSB), 0xF7 (LSB), and 0xF8 (XLSB)
  Wire.beginTransmission(BMP085_ADDRESS);
  Wire.write(0xF6);
  Wire.endTransmission();
  Wire.requestFrom(BMP085_ADDRESS, 3);
  
  // Wait for data to become available
  while(Wire.available() < 3)
    ;
  msb = Wire.read();
  lsb = Wire.read();
  xlsb = Wire.read();
  
  up = (((unsigned long) msb << 16) | ((unsigned long) lsb << 8) | (unsigned long) xlsb) >> (8-OSS);
  
  return up;
}


// Calculate temperature given ut.
// Value returned will be in units of 0.1 deg C
short bmp085GetTemperature(unsigned int ut)
{
  long x1, x2;
  
  x1 = (((long)ut - (long)ac6)*(long)ac5) >> 15;
  x2 = ((long)mc << 11)/(x1 + md);
  b5 = x1 + x2;

  return ((b5 + 8)>>4);  
}

// Calculate pressure given up
// calibration values must be known
// b5 is also required so bmp085GetTemperature(...) must be called first.
// Value returned will be pressure in units of Pa.
long bmp085GetPressure(unsigned long up)
{
  long x1, x2, x3, b3, b6, p;
  unsigned long b4, b7;
  
  b6 = b5 - 4000;
  // Calculate B3
  x1 = (b2 * (b6 * b6)>>12)>>11;
  x2 = (ac2 * b6)>>11;
  x3 = x1 + x2;
  b3 = (((((long)ac1)*4 + x3)<<OSS) + 2)>>2;
  
  // Calculate B4
  x1 = (ac3 * b6)>>13;
  x2 = (b1 * ((b6 * b6)>>12))>>16;
  x3 = ((x1 + x2) + 2)>>2;
  b4 = (ac4 * (unsigned long)(x3 + 32768))>>15;
  
  b7 = ((unsigned long)(up - b3) * (50000>>OSS));
  if (b7 < 0x80000000)
    p = (b7<<1)/b4;
  else
    p = (b7/b4)<<1;
    
  x1 = (p>>8) * (p>>8);
  x1 = (x1 * 3038)>>16;
  x2 = (-7357 * p)>>16;
  p += (x1 + x2 + 3791)>>4;
  
  return p;
}


float getAltitude(long pressure)
{
const float p0 = 101325;     // Pressure at sea level (Pa)
float altitude;

  // Add this into loop(), after you've calculated the pressure
  altitude = (float)44330 * (1 - pow(((float) pressure/p0), 0.190295));

  return altitude;
 }

void forward()
{
  digitalWrite(7, LOW);
  digitalWrite(10, HIGH);
  
  digitalWrite(8, LOW);
  digitalWrite(9, HIGH);
}


void left()
{
  digitalWrite(7, HIGH);
  digitalWrite(10, LOW);
  
  digitalWrite(8, LOW);
  digitalWrite(9, HIGH);
}


void right()
{
  digitalWrite(7, LOW);
  digitalWrite(10, HIGH);
  
  digitalWrite(8, HIGH);
  digitalWrite(9, LOW);
}



void halt()
{
  digitalWrite(7, HIGH);
  digitalWrite(10, HIGH);
  
  digitalWrite(8, HIGH);
  digitalWrite(9, HIGH);
  
}



