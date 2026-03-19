import tkinter as tk
from tkinter import ttk, messagebox
from config import DEFAULT_API_URL, BAUD_RATES, DEFAULT_BAUD
import serial_manager
import api_manager


class ESP32ProvisionerGUI:
    def __init__(self, root):
        self.root = root
        root.title("ESP32 Provisioner")
        self._current_page = 0
        self._total_pages = 0
        self._selected_device_id = None
        self._registration_token = None
        self.setup_gui()

    def setup_gui(self):
        # --- 1. API Login ---
        api_frame = ttk.LabelFrame(self.root, text="1. API Login")
        api_frame.pack(fill="x", padx=10, pady=5)

        tk.Label(api_frame, text="API URL").grid(row=0, column=0, sticky="e", padx=5)
        self.api_entry = tk.Entry(api_frame, width=40)
        self.api_entry.grid(row=0, column=1, padx=5, pady=2, columnspan=2, sticky="we")
        self.api_entry.insert(0, DEFAULT_API_URL)

        tk.Label(api_frame, text="Email").grid(row=1, column=0, sticky="e", padx=5)
        self.email_entry = tk.Entry(api_frame, width=25)
        self.email_entry.grid(row=1, column=1, padx=5, pady=2)
        self.email_entry.insert(0, "alice@test.com")

        tk.Label(api_frame, text="Password").grid(row=2, column=0, sticky="e", padx=5)
        self.password_entry = tk.Entry(api_frame, show="*", width=25)
        self.password_entry.grid(row=2, column=1, padx=5, pady=2)
        self.password_entry.insert(0, "password")

        tk.Button(api_frame, text="Login", command=self.login).grid(row=1, column=2, rowspan=2, padx=10, sticky="ns")

        self.login_status = tk.Label(api_frame, text="Not logged in", fg="grey")
        self.login_status.grid(row=3, column=0, columnspan=3, sticky="w", padx=5, pady=2)

        # --- 2. Serial Connection ---
        serial_frame = ttk.LabelFrame(self.root, text="2. Serial Connection")
        serial_frame.pack(fill="x", padx=10, pady=5)

        tk.Label(serial_frame, text="Port").grid(row=0, column=0, sticky="e", padx=5)
        self.port_var = tk.StringVar()
        ports = serial_manager.list_serial_ports()
        self.port_dropdown = ttk.Combobox(serial_frame, textvariable=self.port_var, values=ports, state="readonly", width=18)
        self.port_dropdown.grid(row=0, column=1, padx=5, pady=2)
        if ports:
            self.port_var.set(ports[0])

        tk.Label(serial_frame, text="Baud").grid(row=0, column=2, sticky="e", padx=5)
        self.baud_var = tk.StringVar(value=DEFAULT_BAUD)
        self.baud_dropdown = ttk.Combobox(serial_frame, textvariable=self.baud_var, values=BAUD_RATES, state="readonly", width=10)
        self.baud_dropdown.grid(row=0, column=3, padx=5, pady=2)

        tk.Button(serial_frame, text="Connect", command=self.connect_serial).grid(row=0, column=4, padx=10)

        self.serial_status = tk.Label(serial_frame, text="Not connected", fg="grey")
        self.serial_status.grid(row=1, column=0, columnspan=5, sticky="w", padx=5)

        self.serial_output = tk.Text(serial_frame, height=6, state="disabled", bg="#1e1e1e", fg="#d4d4d4")
        self.serial_output.grid(row=2, column=0, columnspan=5, padx=5, pady=5, sticky="we")
        serial_manager.serial_callback = self.add_serial_output

        # --- 3. Device Browser ---
        device_frame = ttk.LabelFrame(self.root, text="3. Select Device")
        device_frame.pack(fill="both", expand=True, padx=10, pady=5)

        filter_row = tk.Frame(device_frame)
        filter_row.pack(fill="x", padx=5, pady=4)
        tk.Label(filter_row, text="Name filter:").pack(side="left")
        self.name_filter_var = tk.StringVar()
        self.name_filter_entry = tk.Entry(filter_row, textvariable=self.name_filter_var, width=22)
        self.name_filter_entry.pack(side="left", padx=5)
        self.name_filter_entry.bind("<Return>", lambda _: self.search_devices())
        tk.Button(filter_row, text="Search", command=self.search_devices).pack(side="left")
        tk.Button(filter_row, text="Refresh", command=self.load_devices).pack(side="left", padx=5)

        tree_frame = tk.Frame(device_frame)
        tree_frame.pack(fill="both", expand=True, padx=5)
        self.device_tree = ttk.Treeview(tree_frame, columns=("name",), show="headings", height=7, selectmode="browse")
        self.device_tree.heading("name", text="Device Name")
        self.device_tree.column("name", width=320)
        scrollbar = ttk.Scrollbar(tree_frame, orient="vertical", command=self.device_tree.yview)
        self.device_tree.configure(yscrollcommand=scrollbar.set)
        self.device_tree.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")
        self.device_tree.bind("<<TreeviewSelect>>", self.on_device_select)

        page_row = tk.Frame(device_frame)
        page_row.pack(fill="x", padx=5, pady=4)
        tk.Button(page_row, text="< Prev", command=self.prev_page).pack(side="left")
        self.page_label = tk.Label(page_row, text="Page - / -", width=14)
        self.page_label.pack(side="left", padx=8)
        tk.Button(page_row, text="Next >", command=self.next_page).pack(side="left")

        sel_row = tk.Frame(device_frame)
        sel_row.pack(fill="x", padx=5, pady=4)
        tk.Label(sel_row, text="Selected:").pack(side="left")
        self.selected_label = tk.Label(sel_row, text="None", fg="grey", width=24, anchor="w")
        self.selected_label.pack(side="left", padx=5)
        tk.Label(sel_row, text="Token:").pack(side="left", padx=(10, 0))
        self.token_var = tk.StringVar()
        tk.Entry(sel_row, textvariable=self.token_var, state="readonly", width=22).pack(side="left", padx=5)
        tk.Button(sel_row, text="Copy", command=self.copy_token).pack(side="left")

        # --- 4. Provision via Serial ---
        provision_frame = ttk.LabelFrame(self.root, text="4. Provision via Serial")
        provision_frame.pack(fill="x", padx=10, pady=5)

        # Step A — Wi-Fi
        wifi_frame = ttk.LabelFrame(provision_frame, text="Step A: Connect Wi-Fi")
        wifi_frame.pack(fill="x", padx=5, pady=4)

        tk.Label(wifi_frame, text="Wi-Fi SSID").grid(row=0, column=0, sticky="e", padx=5, pady=2)
        self.ssid_entry = tk.Entry(wifi_frame, width=25)
        self.ssid_entry.grid(row=0, column=1, padx=5, pady=2)

        tk.Label(wifi_frame, text="Wi-Fi Password").grid(row=1, column=0, sticky="e", padx=5, pady=2)
        self.wifi_pass_entry = tk.Entry(wifi_frame, width=25)
        self.wifi_pass_entry.grid(row=1, column=1, padx=5, pady=2)

        self.wifi_btn = tk.Button(wifi_frame, text="Connect Wi-Fi", command=self.send_wifi_credentials)
        self.wifi_btn.grid(row=0, column=2, rowspan=2, padx=15, pady=5, sticky="ns")

        self.wifi_status = tk.Label(wifi_frame, text="", fg="grey")
        self.wifi_status.grid(row=2, column=0, columnspan=3, sticky="w", padx=5, pady=2)

        # Step B — Register (locked until Wi-Fi connected)
        register_frame = ttk.LabelFrame(provision_frame, text="Step B: Register Device")
        register_frame.pack(fill="x", padx=5, pady=4)

        tk.Label(register_frame, text="Token").grid(row=0, column=0, sticky="e", padx=5, pady=2)
        self.reg_token_var = tk.StringVar()
        tk.Entry(register_frame, textvariable=self.reg_token_var, state="readonly", width=30).grid(
            row=0, column=1, padx=5, pady=2)

        self.register_btn = tk.Button(register_frame, text="Register", command=self.send_register,
                                      state="disabled")
        self.register_btn.grid(row=0, column=2, padx=15, pady=5)

        self.register_status = tk.Label(register_frame, text="Waiting for Wi-Fi...", fg="grey")
        self.register_status.grid(row=1, column=0, columnspan=3, sticky="w", padx=5, pady=2)

        # --- 5. Manual Command ---
        command_frame = ttk.LabelFrame(self.root, text="5. Manual Command")
        command_frame.pack(fill="x", padx=10, pady=5)
        self.command_entry = tk.Entry(command_frame, width=52)
        self.command_entry.pack(side="left", padx=5, pady=5)
        tk.Button(command_frame, text="Send", command=self.send_manual_command).pack(side="left", padx=5)

    # --- Helpers ---

    def add_serial_output(self, line):
        self.serial_output.configure(state="normal")
        self.serial_output.insert("end", line + "\n")
        self.serial_output.see("end")
        self.serial_output.configure(state="disabled")
        if "PROVISION:READY" in line:
            self._on_device_provision_ready()
        elif "PROVISION:STARTED" in line:
            self.serial_status.config(text="Device in provision mode", fg="blue")
        elif "PROVISION:WIFI_CONNECTED" in line:
            self._on_wifi_connected()
        elif "PROVISION:WIFI_FAILED" in line:
            self._on_wifi_failed()
        elif "PROVISION:WIFI_BUSY" in line:
            self.wifi_status.config(text="Provisioning busy — try again", fg="red")
        elif "PROVISION:REGISTERED" in line:
            self._on_registered()
        elif "PROVISION:REGISTRATION_FAILED" in line:
            self._on_registration_failed()
        elif "PROVISION:REGISTER_BUSY" in line:
            self.register_status.config(text="Not ready — connect Wi-Fi first", fg="red")

    def _on_device_provision_ready(self):
        self.serial_status.config(text="Device ready — sending start_provision...", fg="blue")
        serial_manager.send_command("start_provision")

    def copy_token(self):
        token = self.token_var.get()
        if token:
            self.root.clipboard_clear()
            self.root.clipboard_append(token)

    # --- Actions ---

    def login(self):
        email = self.email_entry.get()
        password = self.password_entry.get()
        api_url = self.api_entry.get()
        self.login_status.config(text="Logging in...", fg="grey")
        success, msg = api_manager.login_with_session(email, password, api_url)
        if success:
            self.login_status.config(text="Logged in", fg="green")
            self._current_page = 0
            self.load_devices()
        else:
            self.login_status.config(text="Login failed", fg="red")
            messagebox.showerror("Login Error", msg)

    def search_devices(self):
        self._current_page = 0
        self.load_devices()

    def prev_page(self):
        if self._current_page > 0:
            self._current_page -= 1
            self.load_devices()

    def next_page(self):
        if self._current_page < self._total_pages - 1:
            self._current_page += 1
            self.load_devices()

    def load_devices(self):
        api_url = self.api_entry.get()
        name = self.name_filter_var.get().strip() or None
        success, result = api_manager.fetch_devices(api_url, page=self._current_page, size=10, name=name)
        if not success:
            messagebox.showerror("Devices", result)
            return

        self._total_pages = result["totalPages"]
        self.page_label.config(text=f"Page {result['currentPage'] + 1} / {max(self._total_pages, 1)}")

        for row in self.device_tree.get_children():
            self.device_tree.delete(row)
        for d in result["devices"]:
            self.device_tree.insert("", "end", iid=d["id"], values=(d["name"],))

    def on_device_select(self, event):
        sel = self.device_tree.selection()
        if not sel:
            return
        device_id = sel[0]
        device_name = self.device_tree.item(device_id, "values")[0]
        self._selected_device_id = device_id
        self._registration_token = None
        self.selected_label.config(text=device_name, fg="black")
        self.token_var.set("Fetching...")

        api_url = self.api_entry.get()
        token, err = api_manager.get_registration_token(api_url, device_id)
        if err:
            self.token_var.set("")
            self.reg_token_var.set("")
            messagebox.showerror("Token Error", err)
        else:
            self._registration_token = token
            self.token_var.set(token)
            self.reg_token_var.set(token)

    def connect_serial(self):
        port = self.port_var.get()
        baud = self.baud_var.get()
        success, msg = serial_manager.start_serial(port, baud)
        if success:
            self.serial_status.config(text=msg, fg="green")
        else:
            self.serial_status.config(text="Not connected", fg="red")
            messagebox.showerror("Serial Error", msg)

    def send_wifi_credentials(self):
        ssid = self.ssid_entry.get().strip()
        wifi_pass = self.wifi_pass_entry.get()
        if not ssid or not wifi_pass:
            messagebox.showwarning("Wi-Fi", "Enter SSID and password")
            return
        cmd = f"wifi {ssid} {wifi_pass}"
        serial_manager.send_command(cmd)
        self.add_serial_output(f"> {cmd}")
        self.wifi_status.config(text="Connecting...", fg="blue")
        self.wifi_btn.config(state="disabled")

    def send_register(self):
        if not self._registration_token:
            messagebox.showwarning("Register", "No registration token — select a device first")
            return
        cmd = f"register {self._registration_token}"
        serial_manager.send_command(cmd)
        self.add_serial_output(f"> {cmd}")
        self.register_status.config(text="Registering...", fg="blue")
        self.register_btn.config(state="disabled")

    def _on_wifi_connected(self):
        self.wifi_status.config(text="Wi-Fi connected", fg="green")
        self.wifi_btn.config(state="normal")
        self.register_btn.config(state="normal")
        self.register_status.config(text="Ready to register", fg="blue")

    def _on_wifi_failed(self):
        self.wifi_status.config(text="Wi-Fi failed — check credentials", fg="red")
        self.wifi_btn.config(state="normal")

    def _on_registered(self):
        self.register_status.config(text="Device registered successfully", fg="green")
        self.serial_status.config(text="Provisioning complete", fg="green")

    def _on_registration_failed(self):
        self.register_status.config(text="Registration failed — check token / network", fg="red")
        self.register_btn.config(state="normal")

    def send_manual_command(self):
        cmd = self.command_entry.get()
        if cmd:
            serial_manager.send_command(cmd)
            self.add_serial_output(f"> {cmd}")
