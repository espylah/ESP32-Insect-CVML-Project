import { useState } from "react";
import { Card, Col, Container, Row } from "react-bootstrap";
import LoginForm from "../login/LoginForm";
import OtpLoginForm from "../login/OtpLoginForm";


function LoginPage() {
    const [mode, setMode] = useState('password');

    return (
        <Container fluid className="vh-100 p-0">
            <Row className="h-100 m-0">

                <Col
                    xs={12}
                    md={8}
                    className="d-flex align-items-center justify-content-center"
                    style={{ background: '#0b1a2a' }}
                >
                    <Card className="p-4 shadow-lg rounded-4 w-75" style={{ maxWidth: 500 }}>
                        <Card.Body>
                            {mode === 'password'
                                ? <LoginForm onSwitchToOtp={() => setMode('otp')} />
                                : <OtpLoginForm onSwitchToPassword={() => setMode('password')} />}
                        </Card.Body>
                    </Card>
                </Col>

                <Col
                    md={4}
                    className="d-none d-md-flex align-items-center justify-content-center text-white flex-column p-5"
                    style={{
                        background: 'linear-gradient(135deg, #1c2b3a, #082e2b)',
                    }}
                >
                    <h1 className="display-4 fw-bold mb-3">ESP-YLAH PROJECT</h1>
                    <p className="lead">
                        Welcome to the ESP-YLAH dashboard. Please login to continue.
                    </p>
                    <a href='https://github.com/espylah' style={{color:'white'}}>https://github.com/espylah</a>
                </Col>
            </Row>
        </Container>
    );
}


export default LoginPage;
