import requests
from tkinter import messagebox

session = requests.Session()


def _csrf_headers():
    token = session.cookies.get("XSRF-TOKEN")
    return {"X-XSRF-TOKEN": token} if token else {}


def login_with_session(email, password, api_url):
    global session
    if not email or not password:
        return False, "Missing credentials"

    try:
        resp = session.get(f"{api_url}/csrf", timeout=5)
        resp.raise_for_status()
        csrf_token = session.cookies.get("XSRF-TOKEN")
        if not csrf_token:
            return False, "No CSRF token returned"

        headers = {"X-XSRF-TOKEN": csrf_token}
        resp = session.post(f"{api_url}/login", json={"email": email, "password": password}, headers=headers, timeout=5)
        resp.raise_for_status()

        return True, "Login successful"

    except Exception as e:
        return False, str(e)


def fetch_devices(api_url, page=0, size=10, name=None):
    try:
        params = {"state": "UNPROVISIONED", "page": page, "size": size}
        if name:
            params["name"] = name

        resp = session.get(f"{api_url}/devices", params=params, headers=_csrf_headers(), timeout=5)
        resp.raise_for_status()
        data = resp.json()

        return True, {
            "devices": [{"name": d["name"], "id": d["id"]} for d in data["content"]],
            "totalPages": data["totalPages"],
            "totalElements": data["totalElements"],
            "currentPage": data["number"],
        }

    except Exception as e:
        return False, str(e)


def get_registration_token(api_url, device_id):
    try:
        resp = session.post(
            f"{api_url}/devices/{device_id}/registration-token",
            headers=_csrf_headers(),
            timeout=5,
        )
        resp.raise_for_status()
        data = resp.json()
        token = data.get("code")
        if not token:
            return None, "No token in response"
        return token, None

    except Exception as e:
        return None, str(e)
