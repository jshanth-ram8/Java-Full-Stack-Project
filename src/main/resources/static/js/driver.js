/* Driver dashboard: refreshes every 8 seconds */
(async function () {
  const user = await requireRole('DRIVER');
  if (!user) return;

  const profileBox = document.getElementById('profile');
  const tripsBox = document.getElementById('trips');
  const ACTIVE = ['BOOKING_CONFIRMED', 'AMBULANCE_DISPATCHED', 'PATIENT_PICKED_UP'];
  const NEXT = {
    BOOKING_CONFIRMED: { status: 'AMBULANCE_DISPATCHED', label: 'Mark as dispatched' },
    AMBULANCE_DISPATCHED: { status: 'PATIENT_PICKED_UP', label: 'Mark patient picked up' },
    PATIENT_PICKED_UP: { status: 'TRIP_COMPLETED', label: 'Complete trip' }
  };
  let lastJson = '';
  let me = null;

  function profileHtml(p) {
    if (!p.ambulance) {
      return '<div class="empty">No ambulance is assigned to you yet. Ask the admin to assign one.</div>';
    }
    const a = p.ambulance;
    let state, cls;
    if (p.onTrip) { state = 'On a trip'; cls = 'warn'; }
    else if (a.available) { state = 'Available for bookings'; cls = 'good'; }
    else { state = 'Off duty'; cls = ''; }
    return '<div class="panel"><div class="trip-head"><div><h3>' + esc(a.vehicleNumber) + '</h3>' +
      '<div class="muted">' + esc(a.type) + ' ambulance</div></div>' +
      '<span class="badge ' + cls + '">' + state + '</span></div>' +
      '<div class="trip-actions">' +
      '<button class="btn" id="toggle" type="button"' + (p.onTrip ? ' disabled' : '') + '>' +
      (a.available ? 'Go off duty' : 'Go available') + '</button>' +
      (p.onTrip ? '<span class="muted small" style="align-self:center">Finish the current trip to change availability.</span>' : '') +
      '</div></div>';
  }

  function tripHtml(b) {
    const next = NEXT[b.status];
    const u = b.user || {};
    return '<article class="trip s-' + b.status + '">' +
      '<div class="trip-head"><div><h3>' + esc(b.patientName) + '</h3>' +
      '<div class="muted small">Booking #' + b.id + ', ' + esc(formatTime(b.createdAt)) + '</div></div>' +
      '<span class="badge b-' + b.status + '">' + esc(STATUS_LABEL[b.status]) + '</span></div>' +
      '<dl class="facts">' +
      '<div><dt>Pickup</dt><dd>' + esc(b.pickupAddress) + '<br><a href="' + esc(mapsLink(b.pickupAddress)) + '" target="_blank" rel="noopener">Open in Google Maps</a></dd></div>' +
      '<div><dt>Take patient to</dt><dd>' + esc(b.hospital ? b.hospital.name : '') + '<br><span class="muted small">' + esc(b.hospital ? b.hospital.address : '') + '</span></dd></div>' +
      '<div><dt>Contact</dt><dd>' + esc(u.name) + ', <a href="tel:' + esc(u.phone) + '">' + esc(u.phone) + '</a></dd></div>' +
      '<div><dt>Emergency details</dt><dd>' + (b.emergencyDetails ? esc(b.emergencyDetails) : '<span class="muted">None given</span>') + '</dd></div>' +
      '</dl>' +
      (next ? '<div class="trip-actions"><button class="btn btn-red" data-id="' + b.id + '" data-next="' + next.status + '" type="button">' + next.label + '</button></div>' : '') +
      '</article>';
  }

  async function load() {
    try {
      const [profile, trips] = await Promise.all([api('/api/driver/me'), api('/api/driver/bookings')]);
      const json = JSON.stringify([profile, trips]);
      if (json === lastJson) return;
      lastJson = json;
      me = profile;
      profileBox.innerHTML = profileHtml(profile);

      const active = trips.filter(t => ACTIVE.includes(t.status));
      const past = trips.filter(t => !ACTIVE.includes(t.status));
      let html = '';
      if (active.length) html += active.map(tripHtml).join('');
      else html += '<div class="empty">No active trips. New bookings appear here automatically.</div>';
      if (past.length) html += '<h2 style="margin-top:32px">Past trips</h2>' + past.map(tripHtml).join('');
      tripsBox.innerHTML = html;
    } catch (err) {
      if (err.status === 401) { location.href = 'login.html'; return; }
      setMsg('msg', err.message, 'error');
    }
  }

  document.addEventListener('click', async e => {
    const next = e.target.closest('[data-next]');
    const toggle = e.target.closest('#toggle');
    try {
      if (next) {
        next.disabled = true;
        await api('/api/driver/bookings/' + next.dataset.id + '/status', 'PUT', { status: next.dataset.next });
        setMsg('msg', 'Status updated.', 'success');
      } else if (toggle && me && me.ambulance) {
        toggle.disabled = true;
        await api('/api/driver/availability', 'PUT', { available: !me.ambulance.available });
        setMsg('msg', 'Availability updated.', 'success');
      } else {
        return;
      }
      lastJson = '';
      await load();
    } catch (err) {
      setMsg('msg', err.message, 'error');
      lastJson = '';
      await load();
    }
  });

  await load();
  setInterval(load, 8000);
})();
