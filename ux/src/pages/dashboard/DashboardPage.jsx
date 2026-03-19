import { useState, useEffect } from 'react';
import { Badge, Button, Card, Col, Container, Form, Pagination, Row, Spinner, Table } from 'react-bootstrap';
import { useSnackbar } from 'notistack';
import { appFetch } from '../../appFetch';
import PageWrapperComponent from '../PageWrapperComponent';
import CreateDeviceForm from './CreateDeviceForm';

function LastSeenCell({ lastSeenAt }) {
    if (!lastSeenAt) return <span className="text-white-50">Never</span>;
    const seenAt = new Date(lastSeenAt);
    const hoursAgo = (Date.now() - seenAt.getTime()) / (60 * 60 * 1000);
    if (hoursAgo > 24) return <span style={{ color: '#e74c3c' }}>⚠️ {seenAt.toLocaleString()}</span>;
    if (hoursAgo > 12)  return <span style={{ color: '#f39c12' }}>⚠️ {seenAt.toLocaleString()}</span>;
    return <span>{seenAt.toLocaleString()}</span>;
}

function DashboardPage() {
    const { enqueueSnackbar } = useSnackbar();
    const [view, setView] = useState('list');
    const [refreshKey, setRefreshKey] = useState(0);
    const [editDeviceId, setEditDeviceId] = useState(null);
    const [editInitialValues, setEditInitialValues] = useState(null);
    const [pageData, setPageData] = useState({ content: [], totalPages: 0, totalElements: 0, number: 0 });
    const [page, setPage] = useState(0);
    const [filters, setFilters] = useState({ name: '', state: '', runMode: '', enabled: '', online: '' });
    const [loading, setLoading] = useState(false);
    const [hoveredId, setHoveredId] = useState(null);
    const [speciesCache, setSpeciesCache] = useState({});
    const [species, setSpecies] = useState([]);

    useEffect(() => {
        appFetch('/api/species').then(r => r.json()).then(setSpecies);
    }, []);

    useEffect(() => {
        setLoading(true);
        const params = new URLSearchParams({ page, size: 10, sort: 'createdAt,desc' });
        if (filters.name)       params.set('name', filters.name);
        if (filters.state)      params.set('state', filters.state);
        if (filters.runMode)    params.set('runMode', filters.runMode);
        if (filters.enabled !== '') params.set('enabled', filters.enabled);
        if (filters.online !== '')  params.set('online', filters.online);

        appFetch(`/api/devices?${params}`)
            .then(res => res.json())
            .then(data => setPageData(data))
            .finally(() => setLoading(false));
    }, [page, filters, refreshKey]);

    function handleFilterChange(e) {
        const { name, value } = e.target;
        setFilters(prev => ({ ...prev, [name]: value }));
        setPage(0);
    }

    function handleRowEnter(deviceId) {
        setHoveredId(deviceId);
        if (!speciesCache[deviceId]) {
            appFetch(`/api/devices/${deviceId}`)
                .then(res => res.json())
                .then(data => setSpeciesCache(prev => ({ ...prev, [deviceId]: data.targetSpecies ?? [] })));
        }
    }

    function handleRowLeave() {
        setHoveredId(null);
    }

    function handleToggleEnabled(device) {
        const next = !device.enabled;
        appFetch(`/api/devices/${device.id}/enabled`, { method: 'PATCH', body: { enabled: next } })
            .then(res => {
                if (res.ok) {
                    enqueueSnackbar(`Device ${next ? 'enabled' : 'disabled'}`, { variant: 'success' });
                    setPageData(prev => ({
                        ...prev,
                        content: prev.content.map(d => d.id === device.id ? { ...d, enabled: next } : d),
                    }));
                } else {
                    enqueueSnackbar('Failed to update device', { variant: 'error' });
                }
            });
    }

    function handleEdit(device) {
        appFetch(`/api/devices/${device.id}`)
            .then(res => res.json())
            .then(detail => {
                setEditDeviceId(device.id);
                setEditInitialValues({ name: detail.name, runMode: detail.runMode, targetSpecies: detail.targetSpecies });
                setView('edit');
            });
    }

    function paginationItems() {
        const total = pageData.totalPages;
        if (total <= 1) return null;

        const start = Math.max(0, page - 2);
        const end = Math.min(total - 1, page + 2);
        const items = [];

        items.push(<Pagination.Prev key="prev" disabled={page === 0} onClick={() => setPage(p => p - 1)} />);
        if (start > 0) {
            items.push(<Pagination.Item key={0} onClick={() => setPage(0)}>1</Pagination.Item>);
            if (start > 1) items.push(<Pagination.Ellipsis key="el-start" disabled />);
        }
        for (let i = start; i <= end; i++) {
            items.push(
                <Pagination.Item key={i} active={i === page} onClick={() => setPage(i)}>{i + 1}</Pagination.Item>
            );
        }
        if (end < total - 1) {
            if (end < total - 2) items.push(<Pagination.Ellipsis key="el-end" disabled />);
            items.push(<Pagination.Item key={total - 1} onClick={() => setPage(total - 1)}>{total}</Pagination.Item>);
        }
        items.push(<Pagination.Next key="next" disabled={page >= total - 1} onClick={() => setPage(p => p + 1)} />);
        return items;
    }

    const speciesLabels = Object.fromEntries(species.map(s => [s.id, s.description]));

    return (
        <PageWrapperComponent pageContent={
            <Container className="py-4">
                <Card className="p-4">
                    <Row className="mb-3 align-items-center">
                        <Col><h4 className="mb-0 fw-semibold">My Devices</h4></Col>
                        <Col xs="auto" className="text-white-50 small">
                            {pageData.totalElements} device{pageData.totalElements !== 1 ? 's' : ''}
                        </Col>
                        <Col xs="auto">
                            {view === 'list'
                                ? <Button variant="outline-light" size="sm" onClick={() => setView('create')}>New Device</Button>
                                : null
                            }
                        </Col>
                    </Row>

                    {view === 'create' && (
                        <CreateDeviceForm
                            species={species}
                            onSuccess={() => { setRefreshKey(k => k + 1); setView('list'); }}
                            onCancel={() => setView('list')}
                        />
                    )}

                    {view === 'edit' && (
                        <CreateDeviceForm
                            species={species}
                            deviceId={editDeviceId}
                            initialValues={editInitialValues}
                            onSuccess={() => {
                                setRefreshKey(k => k + 1);
                                setSpeciesCache(prev => { const n = { ...prev }; delete n[editDeviceId]; return n; });
                                setView('list');
                            }}
                            onCancel={() => setView('list')}
                        />
                    )}

                    {view === 'list' && <><Row className="mb-3 g-2" data-bs-theme="dark">
                        <Col md={4}>
                            <Form.Control
                                name="name"
                                placeholder="Search by name"
                                value={filters.name}
                                onChange={handleFilterChange}
                            />
                        </Col>
                        <Col md={3}>
                            <Form.Select name="state" value={filters.state} onChange={handleFilterChange}>
                                <option value="">All states</option>
                                <option value="PROVISIONED">Provisioned</option>
                                <option value="UNPROVISIONED">Unprovisioned</option>
                            </Form.Select>
                        </Col>
                        <Col md={3}>
                            <Form.Select name="runMode" value={filters.runMode} onChange={handleFilterChange}>
                                <option value="">All run modes</option>
                                <option value="DEFAULT">Default</option>
                                <option value="ALWAYS_ON">Always On</option>
                                <option value="TRAINING_UPLOADER">Training Uploader</option>
                            </Form.Select>
                        </Col>
                        <Col md={2}>
                            <Form.Select name="enabled" value={filters.enabled} onChange={handleFilterChange}>
                                <option value="">Any status</option>
                                <option value="true">Enabled</option>
                                <option value="false">Disabled</option>
                            </Form.Select>
                        </Col>
                        <Col md={2}>
                            <Form.Select name="online" value={filters.online} onChange={handleFilterChange}>
                                <option value="">All devices</option>
                                <option value="true">Online only</option>
                            </Form.Select>
                        </Col>
                    </Row>

                    <Row>
                        <Col>
                            {loading ? (
                                <div className="text-center py-5"><Spinner animation="border" variant="light" /></div>
                            ) : (
                                <Table variant="dark" hover responsive style={{ background: 'transparent' }}>
                                    <thead>
                                        <tr>
                                            <th>Name</th>
                                            <th>Run Mode</th>
                                            <th>State</th>
                                            <th>Enabled</th>
                                            <th>Last Seen</th>
                                            <th>Created</th>
                                            <th></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {pageData.content.length === 0 ? (
                                            <tr>
                                                <td colSpan={7} className="text-center text-white-50 py-4">No devices found.</td>
                                            </tr>
                                        ) : pageData.content.map(device => (
                                            <>
                                                <tr
                                                    key={device.id}
                                                    onMouseEnter={() => handleRowEnter(device.id)}
                                                    onMouseLeave={handleRowLeave}
                                                >
                                                    <td>{device.name}</td>
                                                    <td className="text-capitalize">{device.runMode?.toLowerCase().replaceAll('_', ' ')}</td>
                                                    <td>
                                                        <Badge bg={device.state === 'PROVISIONED' ? 'success' : 'secondary'}>
                                                            {device.state}
                                                        </Badge>
                                                    </td>
                                                    <td>
                                                        <Badge bg={device.enabled ? 'success' : 'danger'}>
                                                            {device.enabled ? 'Yes' : 'No'}
                                                        </Badge>
                                                    </td>
                                                    <td><LastSeenCell lastSeenAt={device.lastSeenAt} /></td>
                                                    <td>{new Date(device.createdAt).toLocaleDateString()}</td>
                                                    <td className="text-end text-nowrap">
                                                        <Button
                                                            variant="outline-secondary"
                                                            size="sm"
                                                            className="me-1"
                                                            onClick={() => handleToggleEnabled(device)}
                                                        >
                                                            {device.enabled ? 'Disable' : 'Enable'}
                                                        </Button>
                                                        <Button
                                                            variant="outline-light"
                                                            size="sm"
                                                            onClick={() => handleEdit(device)}
                                                        >
                                                            Edit
                                                        </Button>
                                                    </td>
                                                </tr>
                                                {hoveredId === device.id && (
                                                    <tr
                                                        key={`${device.id}-species`}
                                                        onMouseEnter={() => handleRowEnter(device.id)}
                                                        onMouseLeave={handleRowLeave}
                                                        style={{ background: 'rgba(255,255,255,0.04)' }}
                                                    >
                                                        <td colSpan={7} className="py-2 px-3">
                                                            {speciesCache[device.id] === undefined ? (
                                                                <Spinner animation="border" size="sm" variant="light" />
                                                            ) : speciesCache[device.id].length === 0 ? (
                                                                <span className="text-white-50 small">No target species configured</span>
                                                            ) : (
                                                                <div className="d-flex gap-2 flex-wrap">
                                                                    {speciesCache[device.id].map(t => (
                                                                        <Badge
                                                                            key={t.specie}
                                                                            bg="dark"
                                                                            className="border border-secondary"
                                                                            style={{ fontSize: '0.78em' }}
                                                                        >
                                                                                                                            {speciesLabels[t.specie] ?? t.specie} — {t.threshold}%
                                                                        </Badge>
                                                                    ))}
                                                                </div>
                                                            )}
                                                        </td>
                                                    </tr>
                                                )}
                                            </>
                                        ))}
                                    </tbody>
                                </Table>
                            )}
                        </Col>
                    </Row>

                    {pageData.totalPages > 1 && (
                        <Row>
                            <Col className="d-flex justify-content-center" data-bs-theme="dark">
                                <Pagination>{paginationItems()}</Pagination>
                            </Col>
                        </Row>
                    )}</>}
                </Card>
            </Container>
        } />
    );
}

export default DashboardPage;
