/* Admin dashboard */
(async function () {
  const user = await requireRole('ADMIN');
  if (!user) return;

  const $ = id => document.getElementById(id);
  let state = { bookings: [], ambulances: [], hospitals: [], users: [] };

  function render() {
    const { bookings, ambulances, hospitals, users } = state;
    const drivers = users.filter(u => u.role === 'DRIVER');
    const active = bookings.filter(b => ['BOOKING_CONFIRMED', 'AMBULANCE_DISPATCHED', 'PATIENT_PICKED_UP'].includes(b.status)).length;
    const free = ambulances.filter(a => a.available && a.driver).length;
    $('summary').textContent = bookings.length + ' bookings in total, ' + active + ' active. ' +
      free + ' of ' + ambulances.length + ' ambulances are free.';

    $('bookings-body').innerHTML = bookings.length ? bookings.map(b =>
      '<tr><td>' + b.id + '</td><td>' + esc(formatTime(b.createdAt)) + '</td><td>' + esc(b.patientName) + '</td>' +
      '<td>' + esc(b.user ? b.user.name : '') + '<br><span class="muted small">' + esc(b.user ? b.user.phone : '') + '</span></td>' +
      '<td>' + esc(b.pickupAddress) + '</td><td>' + esc(b.hospital ? b.hospital.name : '') + '</td>' +
      '<td>' + esc(b.ambulance ? b.ambulance.vehicleNumber : '') + '</td>' +
      '<td><span class="badge b-' + b.status + '">' + esc(STATUS_LABEL[b.status]) + '</span></td></tr>'
    ).join('') : '<tr><td colspan="8" class="muted">No bookings yet.</td></tr>';

    const driverOptions = selectedId =>
      '<option value="">No driver</option>' + drivers.map(d =>
        '<option value="' + d.id + '"' + (d.id === selectedId ? ' selected' : '') + '>' + esc(d.name) + '</option>').join('');

    $('ambulances-body').innerHTML = ambulances.length ? ambulances.map(a =>
      '<tr><td>' + esc(a.vehicleNumber) + '</td><td>' + esc(a.type) + '</td>' +
      '<td><span class="badge ' + (a.available ? 'good' : 'warn') + '">' + (a.available ? 'Free' : 'Busy or off duty') + '</span></td>' +
      '<td><div class="row-actions"><select id="assign-' + a.id + '" aria-label="Driver for ' + esc(a.vehicleNumber) + '">' +
      driverOptions(a.driver ? a.driver.id : null) + '</select>' +
      '<button class="btn btn-ghost btn-small" data-assign="' + a.id + '" type="button">Save</button></div></td>' +
      '<td><button class="btn btn-danger btn-small" data-del="ambulances" data-id="' + a.id + '" type="button">Delete</button></td></tr>'
    ).join('') : '<tr><td colspan="5" class="muted">No ambulances yet.</td></tr>';

    const keep = $('ambDriver').value;
    $('ambDriver').innerHTML = driverOptions(null);
    $('ambDriver').value = keep;

    $('hospitals-body').innerHTML = hospitals.length ? hospitals.map(h =>
      '<tr><td>' + esc(h.name) + '</td><td>' + esc(h.address) + '</td><td>' + esc(h.phone) + '</td>' +
      '<td><button class="btn btn-danger btn-small" data-del="hospitals" data-id="' + h.id + '" type="button">Delete</button></td></tr>'
    ).join('') : '<tr><td colspan="4" class="muted">No hospitals yet.</td></tr>';

    $('people-body').innerHTML = users.map(u =>
      '<tr><td>' + esc(u.name) + '</td><td>' + esc(u.email) + '</td><td>' + esc(u.phone) + '</td><td>' + esc(u.role) + '</td><td>' +
      (u.role === 'ADMIN' ? '' : '<button class="btn btn-danger btn-small" data-del="users" data-id="' + u.id + '" type="button">Delete</button>') +
      '</td></tr>'
    ).join('');
  }

  async function load() {
    try {
      const [bookings, ambulances, hospitals, users] = await Promise.all([
        api('/api/admin/bookings'), api('/api/admin/ambulances'), api('/api/hospitals'), api('/api/admin/users')
      ]);
      state = { bookings, ambulances, hospitals, users };
      render();
    } catch (err) {
      if (err.status === 401) { location.href = 'login.html'; return; }
      setMsg('msg', err.message, 'error');
    }
  }

  /** Runs an action, shows the result, and reloads the data. */
  async function run(action, okText) {
    try {
      await action();
      setMsg('msg', okText, 'success');
      await load();
      return true;
    } catch (err) {
      setMsg('msg', err.message, 'error');
      return false;
    }
  }

  // tabs
  document.querySelectorAll('.tabs button').forEach(btn => btn.addEventListener('click', () => {
    document.querySelectorAll('.tabs button').forEach(b => b.setAttribute('aria-selected', String(b === btn)));
    document.querySelectorAll('.tab-panel').forEach(p => { p.hidden = p.id !== 'tab-' + btn.dataset.tab; });
    setMsg('msg', '');
  }));

  // delete and assign buttons
  document.addEventListener('click', async e => {
    const del = e.target.closest('[data-del]');
    if (del) {
      if (!confirm('Delete this record? This cannot be undone.')) return;
      await run(() => api('/api/admin/' + del.dataset.del + '/' + del.dataset.id, 'DELETE'), 'Deleted.');
      return;
    }
    const assign = e.target.closest('[data-assign]');
    if (assign) {
      const value = $('assign-' + assign.dataset.assign).value;
      await run(() => api('/api/admin/ambulances/' + assign.dataset.assign + '/driver', 'PUT',
        { driverId: value ? Number(value) : null }), 'Driver updated.');
    }
  });

  // add forms
  function bindForm(formId, send, okText) {
    $(formId).addEventListener('submit', async e => {
      e.preventDefault();
      const ok = await run(send, okText);
      if (ok) $(formId).reset();
    });
  }
  bindForm('form-ambulance', () => api('/api/admin/ambulances', 'POST', {
    vehicleNumber: $('vehicleNumber').value,
    type: $('ambType').value,
    driverId: $('ambDriver').value ? Number($('ambDriver').value) : null
  }), 'Ambulance added.');
  bindForm('form-hospital', () => api('/api/admin/hospitals', 'POST', {
    name: $('hosName').value, address: $('hosAddress').value, phone: $('hosPhone').value
  }), 'Hospital added.');
  bindForm('form-driver', () => api('/api/admin/drivers', 'POST', {
    name: $('drvName').value, email: $('drvEmail').value, phone: $('drvPhone').value, password: $('drvPassword').value
  }), 'Driver added.');

  await load();
  setInterval(load, 10000);
})();
