import { Container, Nav, Navbar } from 'react-bootstrap';
import { useNavigate } from 'react-router';
import { appFetch } from '../appFetch';

function PageWrapperComponent({ pageContent }) {
    const nav = useNavigate();

    function handleLogout() {
        appFetch('/logout', { method: 'POST' }).finally(() => nav('/login'));
    }

    return (
        <div className="min-vh-100" style={{ background: '#0b1a2a' }}>
            <Navbar
                fixed="top"
                expand="lg"
                variant="dark"
                style={{ background: 'linear-gradient(135deg, #1c2b3a, #082e2b)' }}
            >
                <Container>
                    <Navbar.Brand href="/" className="fw-bold">ESP-YLAH</Navbar.Brand>
                    <Navbar.Toggle aria-controls="main-nav" />
                    <Navbar.Collapse id="main-nav">
                        <Nav className="me-auto">
                            <Nav.Link href="/">Dashboard</Nav.Link>
                        </Nav>
                        <Nav>
                            <Nav.Link onClick={handleLogout}>Logout</Nav.Link>
                        </Nav>
                    </Navbar.Collapse>
                </Container>
            </Navbar>

            <div style={{ paddingTop: '4.5rem' }}>
                {pageContent}
            </div>
        </div>
    );
}

export default PageWrapperComponent;
