/* Shared helpers used by every page */

const STAGES = [
  { key: 'BOOKING_CONFIRMED', label: 'Booking confirmed' },
  { key: 'AMBULANCE_DISPATCHED', label: 'Ambulance dispatched' },
  { key: 'PATIENT_PICKED_UP', label: 'Patient picked up' },
  { key: 'TRIP_COMPLETED', label: 'Trip completed' }
];
const STATUS_LABEL = Object.fromEntries(STAGES.map(s => [s.key, s.label]));
STATUS_LABEL.CANCELLED = 'Cancelled';

const HOME = { USER: 'book.html', DRIVER: 'driver.html', ADMIN: 'admin.html' };

const LOGO = '<svg viewBox="0 0 100 100" aria-hidden="true"><rect width="100" height="100" rx="22" fill="#C8102E"/>' +
  '<path d="M40 18h20v22h22v20H60v22H40V60H18V40h22z" fill="#fff"/></svg>';

/** Calls the backend. Throws an Error with a readable message when the server says no. */
async function api(path, method = 'GET', body) {
  const res = await fetch(path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch (e) { /* response was not JSON */ }
  if (!res.ok) {
    const err = new Error((data && data.message) || 'Something went wrong. Please try again.');
    err.status = res.status;
    throw err;
  }
  return data;
}

/** Escapes text before it goes into innerHTML. */
function esc(value) {
  return String(value == null ? '' : value).replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function formatTime(iso) {
  return iso ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '';
}

/** Google Maps link for an address, or for GPS coordinates if the text contains them. */
function mapsLink(address) {
  const m = String(address).match(/(-?\d{1,3}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)/);
  const query = m ? m[1] + ',' + m[2] : address;
  return 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(query);
}

function setMsg(id, text, kind) {
  const el = document.getElementById(id);
  if (!el) return;
  el.className = text ? 'msg ' + kind : '';
  el.textContent = text || '';
  if (text) el.scrollIntoView({ block: 'nearest' });
}

async function loadUser() {
  try { return await api('/api/auth/me'); } catch (e) { return null; }
}

function renderNav(user) {
  const links = [];
  if (!user) links.push(['login.html', 'Log in'], ['register.html', 'Register']);
  else if (user.role === 'USER') links.push(['book.html', 'Book'], ['my-bookings.html', 'My bookings']);
  else if (user.role === 'DRIVER') links.push(['driver.html', 'Driver dashboard']);
  else if (user.role === 'ADMIN') links.push(['admin.html', 'Admin dashboard']);

  const current = location.pathname.split('/').pop() || 'index.html';
  const bar = document.getElementById('topbar');
  if (!bar) return;
  bar.innerHTML =
    '<div class="wrap bar"><a class="brand" href="index.html">' + LOGO + '<span>Ambulance Booking</span></a><nav>' +
    links.map(([href, label]) =>
      '<a href="' + href + '"' + (href === current ? ' aria-current="page"' : '') + '>' + label + '</a>').join('') +
    (user ? '<button class="linklike" id="logout" type="button">Log out (' + esc(user.name.split(' ')[0]) + ')</button>' : '') +
    '</nav></div>';

  const logout = document.getElementById('logout');
  if (logout) {
    logout.addEventListener('click', async () => {
      try { await api('/api/auth/logout', 'POST'); } catch (e) { /* ignore */ }
      location.href = 'index.html';
    });
  }
}

/** Use at the top of a protected page. Redirects if the user isn't logged in or has the wrong role. */
async function requireRole(role) {
  const user = await loadUser();
  renderNav(user);
  if (!user) { location.href = 'login.html'; return null; }
  if (user.role !== role) { location.href = HOME[user.role]; return null; }
  return user;
}

/** The four-step progress bar used on the patient's trip cards. */
function stepper(status) {
  if (status === 'CANCELLED') return '';
  const idx = STAGES.findIndex(s => s.key === status);
  return '<ol class="steps" aria-label="Trip progress">' + STAGES.map((s, i) => {
    let cls = '';
    if (status === 'TRIP_COMPLETED' || i < idx) cls = 'done';
    else if (i === idx) cls = 'current';
    return '<li class="' + cls + '"' + (cls === 'current' ? ' aria-current="step"' : '') + '>' + s.label + '</li>';
  }).join('') + '</ol>';
}
