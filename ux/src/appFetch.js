/**
 * SPA-friendly fetch wrapper that automatically includes:
 * Credentials (cookies)
 * X-XSRF-TOKEN header
 */
export async function appFetch(url, options = {}) {

    const defaultOptions = {
        credentials: 'include',
        headers: {}
    };

    const opts = { ...defaultOptions, ...options };

    const xsrfToken = document.cookie
        .split('; ')
        .find(row => row.startsWith('XSRF-TOKEN='))
        ?.split('=')[1];

    if (xsrfToken) {
        opts.headers['X-XSRF-TOKEN'] = xsrfToken;
    }

    // Default Content-Type if sending JSON
    if (!opts.headers['Content-Type'] && opts.body && typeof opts.body === 'object') {
        opts.headers['Content-Type'] = 'application/json';
        opts.body = JSON.stringify(opts.body);
    }

    return fetch(url, opts);
}