/* "My bookings" page: shows trips and refreshes every 5 seconds */
(async function () {
  const user = await requireRole('USER');
  if (!user) return;

  const list = document.getElementById('list');
  let lastJson = '';

  if (new URLSearchParams(location.search).get('new')) {
    setMsg('msg', 'Ambulance booked. The driver has been assigned.', 'success');
  }

  function card(b) {
    const amb = b.ambulance;
    const driver = amb && amb.driver;
    const canCancel = b.status === 'BOOKING_CONFIRMED' || b.status === 'AMBULANCE_DISPATCHED';
    return '<article class="trip s-' + b.status + '">' +
      '<div class="trip-head"><div><h3>' + esc(b.patientName) + '</h3>' +
      '<div class="muted small">Booking #' + b.id + ', ' + esc(formatTime(b.createdAt)) + '</div></div>' +
      '<span class="badge b-' + b.status + '">' + esc(STATUS_LABEL[b.status]) + '</span></div>' +
      stepper(b.status) +
      '<dl class="facts">' +
      '<div><dt>Pickup</dt><dd>' + esc(b.pickupAddress) + '</dd></div>' +
      '<div><dt>Going to</dt><dd>' + esc(b.hospital ? b.hospital.name : '') + '</dd></div>' +
      '<div><dt>Ambulance</dt><dd>' + (amb ? esc(amb.vehicleNumber) + ' (' + esc(amb.type) + ')' : 'Not assigned') + '</dd></div>' +
      '<div><dt>Driver</dt><dd>' + (driver
        ? esc(driver.name) + ', <a href="tel:' + esc(driver.phone) + '">' + esc(driver.phone) + '</a>'
        : 'Not assigned') + '</dd></div>' +
      '</dl>' +
      (canCancel ? '<div class="trip-actions"><button class="btn btn-danger btn-small" data-cancel="' + b.id + '" type="button">Cancel booking</button></div>' : '') +
      '</article>';
  }

  async function load() {
    try {
      const bookings = await api('/api/bookings/my');
      const json = JSON.stringify(bookings);
      if (json === lastJson) return;       // nothing changed, don't redraw
      lastJson = json;
      list.innerHTML = bookings.length
        ? bookings.map(card).join('')
        : '<div class="empty">You have no bookings yet. <a href="book.html">Book an ambulance</a></div>';
    } catch (err) {
      if (err.status === 401) { location.href = 'login.html'; return; }
      setMsg('msg', err.message, 'error');
    }
  }

  list.addEventListener('click', async e => {
    const btn = e.target.closest('[data-cancel]');
    if (!btn) return;
    if (!confirm('Cancel this booking? The ambulance will be released.')) return;
    btn.disabled = true;
    try {
      await api('/api/bookings/' + btn.dataset.cancel + '/cancel', 'PUT');
      setMsg('msg', 'Booking cancelled.', 'success');
      lastJson = '';
      await load();
    } catch (err) {
      setMsg('msg', err.message, 'error');
      btn.disabled = false;
    }
  });

  await load();
  setInterval(load, 5000);
})();
