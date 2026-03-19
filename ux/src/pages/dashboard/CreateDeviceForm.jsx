import { useState } from 'react';
import { Button, Col, Form, Row } from 'react-bootstrap';
import { useSnackbar } from 'notistack';
import { appFetch } from '../../appFetch';

const SPECIES_OPTIONS = [
    { value: 'APIS_MELLIFERA', label: 'Apis mellifera (Honey Bee)' },
    { value: 'VESPA_CABRO', label: 'Vespa crabro (European Hornet)' },
    { value: 'VESPA_VELUTINA_NIGRITHORAX', label: 'Vespa velutina nigrithorax (Asian Hornet)' },
];

const RUN_MODE_OPTIONS = [
    { value: 'DEFAULT', label: 'Default' },
    { value: 'ALWAYS_ON', label: 'Always On' },
    { value: 'TRAINING_UPLOADER', label: 'Training Uploader' },
];

const emptySpeciesRow = () => ({ specie: 'APIS_MELLIFERA', threshold: 75 });

function CreateDeviceForm({ onSuccess, onCancel, deviceId = null, initialValues = null }) {
    const editing = deviceId !== null;
    const { enqueueSnackbar } = useSnackbar();
    const [name, setName] = useState(initialValues?.name ?? '');
    const [runmode, setRunmode] = useState(initialValues?.runMode ?? 'DEFAULT');
    const [targetSpecies, setTargetSpecies] = useState(
        initialValues?.targetSpecies?.length
            ? initialValues.targetSpecies.map(t => ({ specie: t.specie, threshold: t.threshold }))
            : [emptySpeciesRow()]
    );
    const [submitting, setSubmitting] = useState(false);

    function handleSpeciesChange(index, field, value) {
        setTargetSpecies(prev => prev.map((row, i) =>
            i === index ? { ...row, [field]: value } : row
        ));
    }

    function addSpeciesRow() {
        setTargetSpecies(prev => [...prev, emptySpeciesRow()]);
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
            {targetSpecies.map((row, index) => (
                <Row key={index} className="mb-2 align-items-center">
                    <Col md={6}>
                        <Form.Select
                            value={row.specie}
                            onChange={e => handleSpeciesChange(index, 'specie', e.target.value)}
                        >
                            {SPECIES_OPTIONS.map(opt => (
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
            ))}
            <Button variant="outline-secondary" size="sm" className="mb-4 mt-1" onClick={addSpeciesRow}>
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
