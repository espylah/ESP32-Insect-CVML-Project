import { useEffect, useState } from 'react';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import { appFetch } from '../../appFetch';
import { useNavigate } from 'react-router-dom';
import { useSnackbar } from 'notistack';

function LoginForm({ onSwitchToOtp }) {

    const [form, setForm] = useState({ email: '', password: '' });
    const nav = useNavigate();

    useEffect(() => {
        fetch("/api/csrf", { method: 'GET' });
    }, []);

    const { enqueueSnackbar } = useSnackbar();

    return (
        <Form className="w-100">
            <h4 className="mb-4 fw-semibold">Sign In</h4>

            <Form.Group className="mb-3" controlId="formEmail">
                <Form.Label>Email</Form.Label>
                <Form.Control type="email" placeholder="Enter email" onChange={(e) => {
                    setForm({ ...form, email: e.target.value });
                }} />
            </Form.Group>

            <Form.Group className="mb-4" controlId="formPassword">
                <Form.Label>Password</Form.Label>
                <Form.Control type="password" placeholder="Password" onChange={(e) => {
                    setForm({ ...form, password: e.target.value });
                }} />
            </Form.Group>

            <Button variant="info" type="submit" className="w-100 btn-login" onClick={(e) => {
                e.preventDefault();
                appFetch("/api/login", {
                    method: 'POST',
                    credentials: 'include',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(form),
                }).then((res) => {
                    if (res.ok) {
                        enqueueSnackbar("Logged In Successfully", { variant: 'success' });
                        nav("/");
                    } else if (res.status === 401) {
                        enqueueSnackbar("Bad Credentials", {
                            variant: 'error',
                            anchorOrigin: { vertical: 'bottom', horizontal: 'center' },
                        });
                    }
                });
            }}>
                LOGIN
            </Button>

            <div className="d-flex justify-content-between align-items-center mt-3">
                <Button variant="link" className="p-0 btn-useotp" onClick={onSwitchToOtp}>
                    Email me an OTP instead.
                </Button>
                <Button className="btn-signup">
                    No Account? Sign Up
                </Button>
            </div>
        </Form>
    );
}


export default LoginForm;