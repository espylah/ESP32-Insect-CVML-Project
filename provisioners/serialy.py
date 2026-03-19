import serial
import threading
import sys

# Change this to match your ESP32 USB port
SERIAL_PORT = "/dev/ttyACM0"  # or "COM3" on Windows
BAUDRATE = 115200

try:
    ser = serial.Serial(SERIAL_PORT, BAUDRATE, timeout=0.1)
    print(f"Connected to {SERIAL_PORT}")
except Exception as e:
    print(f"Failed to open serial port: {e}")
    sys.exit(1)

# Thread to read from ESP32 and print to console
def read_from_esp32():
    while True:
        try:
            line = ser.readline().decode(errors="ignore")
            if line:
                print(line, end='')  # already has newline
        except Exception as e:
            print(f"Read error: {e}")

# Thread to read from user input and send to ESP32
def write_to_esp32():
    while True:
        try:
            user_input = input()
            ser.write((user_input + "\n").encode())
        except Exception as e:
            print(f"Write error: {e}")

# Start threads
threading.Thread(target=read_from_esp32, daemon=True).start()
threading.Thread(target=write_to_esp32, daemon=True).start()

# Keep main thread alive
try:
    while True:
        pass
except KeyboardInterrupt:
    print("Exiting bridge")
