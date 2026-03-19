import { useState } from 'react';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Alert from 'react-bootstrap/Alert';
import { appFetch } from '../../appFetch';
import { useNavigate } from 'react-router-dom';
import { useSnackbar } from 'notistack';

function OtpLoginForm({ onSwitchToPassword }) {
    const [step, setStep] = useState('request');
    const [email, setEmail] = useState('');
    const [token, setToken] = useState('');
    const [demoToken, setDemoToken] = useState(null);
    const nav = useNavigate();
    const { enqueueSnackbar } = useSnackbar();

    function handleRequest(e) {
        e.preventDefault();
        appFetch('/api/otp/request', {
            method: 'POST',
            body: JSON.stringify({ email:email }),
            headers:{"content-type":"application/json"}
        }).then(async (res) => {
            if (res.ok) {
                const data = await res.json();
                setDemoToken(data.token);
                setStep('verify');
            } else {
                enqueueSnackbar('If an account exists, a code has been sent', {
                    variant: 'info',
                    anchorOrigin: { vertical: 'bottom', horizontal: 'center' },
                });
            }
        });
    }

    function handleLogin(e) {
        e.preventDefault();
        appFetch('/api/otp/login', {
            method: 'POST',
            body: JSON.stringify({ token }),
        }).then((res) => {
            if (res.ok) {
                enqueueSnackbar('Logged In Successfully', { variant: 'success' });
                nav('/');
            } else {
                enqueueSnackbar('Invalid or expired code', {
                    variant: 'error',
                    anchorOrigin: { vertical: 'bottom', horizontal: 'center' },
                });
            }
        });
    }

    return (
        <Form className="w-100">
            <h4 className="mb-4 fw-semibold">Sign In with Code</h4>

            {step === 'request' && (
                <>
                    <Form.Group className="mb-3" controlId="otpEmail">
                        <Form.Label>Email</Form.Label>
                        <Form.Control
                            type="email"
                            placeholder="Enter email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                    </Form.Group>

                    <Button variant="info" className="w-100 mb-3 btn-login" onClick={handleRequest}>
                        SEND CODE
                    </Button>
                </>
            )}

            {step === 'verify' && (
                <>
                    {demoToken && (
                        <Alert variant="info" className="mb-3">
                            <strong>Demo mode:</strong> your code is <code>{demoToken}</code>
                        </Alert>
                    )}

                    <Form.Group className="mb-4" controlId="otpToken">
                        <Form.Label>One-Time Code</Form.Label>
                        <Form.Control
                            type="text"
                            placeholder="Enter code"
                            value={token}
                            onChange={(e) => setToken(e.target.value)}
                        />
                    </Form.Group>

                    <Button variant="info" className="w-100 mb-3 btn-login" onClick={handleLogin}>
                        LOGIN
                    </Button>

                    <Button variant="link" className="p-0" onClick={() => setStep('request')}>
                        Request a new code
                    </Button>
                </>
            )}

            <div className="d-flex justify-content-start mt-3">
                <Button variant="link" className="p-0 btn-useotp" onClick={onSwitchToPassword}>
                    Use password instead
                </Button>
            </div>
        </Form>
    );
}

export default OtpLoginForm;