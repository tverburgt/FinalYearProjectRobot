/* Linksprite */

#include <SoftwareSerial.h>
#include <Wire.h>
#define BMP085_ADDRESS 0x77  // I2C address of BMP085
byte incomingbyte;
SoftwareSerial mySerial = SoftwareSerial(13, 4); //SoftwareSerial(rx of arduino, tx of arduino)
//SoftwareSerial bluetooth = SoftwareSerial(5, 6); //Bluetooth UART interface. rx = 5 & tx = 6 of ATMega328-Pu
int a=0x0000,j=0,k=0,count=0; //Read Starting address       
uint8_t MH,ML;
boolean EndFlag=0;

const unsigned char OSS = 0;  // Oversampling Setting
int flag = 0; //A flag used to determine whethere the WiFi adapter is connected to WiFi modem.
// Calibration values
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

short xtemperature;
long pressure;

const float p0 = 101325;     // Pressure at sea level (Pa)
float altitude;

void bmp085Calibration();                      
void SendResetCmd();
void SendTakePhotoCmd();
void SendReadDataCmd();
void StopTakePhotoCmd();
void  SendDataViaWiFi(float, short, long, float);
void connectToWiFiModem();    //Function used to connect WiFi adapter to WiFi modem.
int x = 0;

int z = 0;


void setup()
{ 
  Serial.begin(9600);  //Don't forget to change this as the baud rate between the computer and ATMEL chip is 9600
   Wire.begin();
   bmp085Calibration(); //Calibrates the parameters within the bmp083 sensor.
}

void loop() 
{         
             xtemperature = bmp085GetTemperature(bmp085ReadUT());
             pressure = bmp085GetPressure(bmp085ReadUP());
             altitude = getAltitude(pressure);
             short temperature = xtemperature*0.1;
             float batteryPower =((analogRead(A3)/1023 ) * 100); 
             
             if(flag = 0){          //If WiFi is not connected.
                  connectToWiFiModem();
                  flag = 1;
             }

             
             
             
             
             SendDataViaWiFi(batteryPower, temperature, pressure, altitude);
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
  while(!Wire.available());
    
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
  
  // Wait at least 5ms
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
 
 void connectToWiFiModem()
 {
  Serial.write("AT+RST\r\n"); //Reset ESP8266 to default configurations.
  delay(3000);   //Create a delay to ensure reset in fully complete.
  
  Serial.write("AT+CWMODE=1");  //Configure the module to be a station.
  Serial.write("\r\n");
  
  //Command to connect to Wi-Fi modem or Wi-Fi server.
  //The command is not printed out in one line because of interferences
  //caused by the "" in the command.
  //Connect to WiFi modem.
  Serial.write("AT+CWJAP=");
  Serial.write('"');
  Serial.write("eircom40887457");
  Serial.write('"');
  Serial.write(',');
  Serial.write('"');
  Serial.write("a80d07791da4");
  Serial.write('"');
  Serial.write("\r\n");
   
  delay(3000);  //3 Second to ensure the module fully connected. 
 }
 
 void SendDataViaWiFi(float batteryPower, short temperature, long pressure, float altitude)
 {
   delay(3000);
  //Used to set-up multiple connections. Must be always set during boot up.
  Serial.write("AT+CIPMUX=1\r\n");
  
  delay(3000);
  
  //Connect to tixgy.com on channel 0 using TCP on port 80.
  //The command is not printed out in one line because of interferences
  //caused by the "" in the command.
  Serial.write("AT+CIPSTART=");
  Serial.write("0,");   //Chaneel number
  Serial.write('"');
  Serial.write("TCP");  //Chosen protocol
  Serial.write('"');
  Serial.write(',');
  Serial.write('"');
  Serial.write("tixgy.com");  //Website
  Serial.write('"');
  Serial.write(',');
  Serial.write("80\r\n");  //Port number.
  
  //3 Second delay created to ensure previous command completes.
  delay(3000);  
  
  //Specifies the number of bytes send to channel 0.
  Serial.write("AT+CIPSEND=0,110\r\n");
  
  //The various strings with the parameters are concatenated. 
  //They are concatenated this way because the parameters change.
  //GET http://www.tixgy.com/DITrobot.php?param1=" + 300 + "&param2=" + 400 + " HTTP/1.0;
  String request = String("GET http://www.tixgy.com/DITrobot.php?param1=");
  request = request + batteryPower;
  request = request + "&param2=";
  request = request + temperature;
  request = request + "&param3=";
  request = request + pressure;
  request = request + "&param4=";
  request = request + altitude;
  request = request + " HTTP/1.0";

  Serial.print(request);
  //The newline and carriage return are entered encase data entered is not
  //shorter than the specified value. These fill in the empty spaces.
  Serial.write("\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n");  
  
  //Used to display display the received.
  while(Serial.available()>0){
    Serial.print((char)Serial.read());
  }
  Serial.write("\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n");   
 }

 
 
 

 


