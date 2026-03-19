import { useState, useEffect } from 'react';
import { Button, Col, Form, Row } from 'react-bootstrap';
import { useSnackbar } from 'notistack';
import { appFetch } from '../../appFetch';

const RUN_MODE_OPTIONS = [
    { value: 'DEFAULT', label: 'Default' },
    { value: 'ALWAYS_ON', label: 'Always On' },
    { value: 'TRAINING_UPLOADER', label: 'Training Uploader' },
];

function CreateDeviceForm({ onSuccess, onCancel, deviceId = null, initialValues = null, species = [] }) {
    const editing = deviceId !== null;
    const { enqueueSnackbar } = useSnackbar();
    const speciesOptions = species.map(s => ({ value: s.id, label: s.description }));
    const [name, setName] = useState(initialValues?.name ?? '');
    const [runmode, setRunmode] = useState(initialValues?.runMode ?? 'DEFAULT');
    const [targetSpecies, setTargetSpecies] = useState(
        initialValues?.targetSpecies?.length
            ? initialValues.targetSpecies.map(t => ({ specie: t.specie, threshold: t.threshold }))
            : []
    );
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        if (!editing && species.length > 0 && targetSpecies.length === 0) {
            setTargetSpecies([{ specie: species[0].id, threshold: 75 }]);
        }
    }, [species.length]); // eslint-disable-line react-hooks/exhaustive-deps

    function handleSpeciesChange(index, field, value) {
        setTargetSpecies(prev => prev.map((row, i) =>
            i === index ? { ...row, [field]: value } : row
        ));
    }

    function addSpeciesRow() {
        setTargetSpecies(prev => {
            const taken = new Set(prev.map(r => r.specie));
            const next = speciesOptions.find(opt => !taken.has(opt.value));
            if (!next) return prev;
            return [...prev, { specie: next.value, threshold: 75 }];
        });
    }

    function removeSpeciesRow(index) {
        setTargetSpecies(prev => prev.filter((_, i) => i !== index));
    }

    async function handleSubmit(e) {
        e.preventDefault();
        if (!name.trim()) {
            enqueueSnackbar('Device name is required', { variant: 'warning' });
            return;
        }
        setSubmitting(true);
        try {
            const body = {
                name: name.trim(),
                runmode,
                targetSpecies: targetSpecies.map(row => ({
                    specie: row.specie,
                    threshold: parseFloat(row.threshold),
                })),
            };
            const res = editing
                ? await appFetch(`/api/devices/${deviceId}`, { method: 'PUT', body })
                : await appFetch('/api/devices/create', { method: 'POST', body });

            if (res.ok) {
                enqueueSnackbar(editing ? 'Device updated' : 'Device created', { variant: 'success' });
                onSuccess();
            } else {
                enqueueSnackbar(editing ? 'Failed to update device' : 'Failed to create device', { variant: 'error' });
            }
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <Form onSubmit={handleSubmit} data-bs-theme="dark">
            <h5 className="mb-4 fw-semibold">{editing ? 'Edit Device' : 'New Device'}</h5>

            <Row className="mb-3">
                <Col md={6}>
                    <Form.Group>
                        <Form.Label>Name</Form.Label>
                        <Form.Control
                            type="text"
                            placeholder="e.g. Hive Sensor 1"
                            value={name}
                            onChange={e => setName(e.target.value)}
                        />
                    </Form.Group>
                </Col>
                <Col md={6}>
                    <Form.Group>
                        <Form.Label>Run Mode</Form.Label>
                        <Form.Select value={runmode} onChange={e => setRunmode(e.target.value)}>
                            {RUN_MODE_OPTIONS.map(opt => (
                                <option key={opt.value} value={opt.value}>{opt.label}</option>
                            ))}
                        </Form.Select>
                    </Form.Group>
                </Col>
            </Row>

            <Form.Label>Target Species</Form.Label>
            {targetSpecies.map((row, index) => {
                const takenByOthers = new Set(
                    targetSpecies.filter((_, i) => i !== index).map(r => r.specie)
                );
                return (
                <Row key={index} className="mb-2 align-items-center">
                    <Col md={6}>
                        <Form.Select
                            value={row.specie}
                            onChange={e => handleSpeciesChange(index, 'specie', e.target.value)}
                        >
                            {speciesOptions.filter(opt => !takenByOthers.has(opt.value) || opt.value === row.specie).map(opt => (
                                <option key={opt.value} value={opt.value}>{opt.label}</option>
                            ))}
                        </Form.Select>
                    </Col>
                    <Col md={4}>
                        <Form.Control
                            type="number"
                            min={0}
                            max={100}
                            step={1}
                            placeholder="Threshold (0–100)"
                            value={row.threshold}
                            onChange={e => handleSpeciesChange(index, 'threshold', e.target.value)}
                        />
                    </Col>
                    <Col md={2}>
                        {targetSpecies.length > 1 && (
                            <Button
                                variant="outline-danger"
                                size="sm"
                                onClick={() => removeSpeciesRow(index)}
                            >
                                Remove
                            </Button>
                        )}
                    </Col>
                </Row>
                );
            })}
            <Button variant="outline-secondary" size="sm" className="mb-4 mt-1" onClick={addSpeciesRow} disabled={targetSpecies.length >= speciesOptions.length}>
                + Add Species
            </Button>

            <div className="d-flex gap-2">
                <Button type="submit" variant="primary" disabled={submitting}>
                    {submitting ? (editing ? 'Saving…' : 'Creating…') : (editing ? 'Save Changes' : 'Create Device')}
                </Button>
                <Button variant="outline-light" onClick={onCancel} disabled={submitting}>
                    Cancel
                </Button>
            </div>
        </Form>
    );
}

export default CreateDeviceForm;
