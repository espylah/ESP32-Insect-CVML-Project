import serial
import threading
from tkinter import messagebox

ser = None
serial_callback = None  # Optional callback to send serial lines to GUI


def list_serial_ports():
    from serial.tools import list_ports
    ports = list_ports.comports()
    return [port.device for port in ports]


def start_serial(port, baud):
    global ser
    try:
        ser = serial.Serial(port, int(baud), timeout=0.5)
        threading.Thread(target=serial_read_thread, daemon=True).start()
        return True, f"Connected to {port} at {baud} baud"
    except Exception as e:
        return False, str(e)


def serial_read_thread():
    while ser:
        try:
            line = ser.readline().decode(errors="ignore").strip()
            if not line:
                continue
            if serial_callback:
                serial_callback(line)
            else:
                print(line)
        except Exception as e:
            print(f"Serial read error: {e}")


def send_command(cmd):
    if ser:
        ser.write((cmd + "\r\n").encode())
    else:
        messagebox.showwarning("Serial", "Serial port not connected")
