/* Book page */
(async function () {
  const user = await requireRole('USER');
  if (!user) return;

  const select = document.getElementById('hospital');
  try {
    const hospitals = await api('/api/hospitals');
    select.innerHTML = hospitals.length
      ? '<option value="">Choose a hospital</option>' +
        hospitals.map(h => '<option value="' + h.id + '">' + esc(h.name) + ' (' + esc(h.address) + ')</option>').join('')
      : '<option value="">No hospitals added yet</option>';
  } catch (err) {
    setMsg('msg', err.message, 'error');
  }

  document.getElementById('locate').addEventListener('click', () => {
    if (!navigator.geolocation) {
      setMsg('msg', 'Location is not available in this browser. Type the address instead.', 'error');
      return;
    }
    navigator.geolocation.getCurrentPosition(pos => {
      const field = document.getElementById('pickupAddress');
      const point = 'GPS location: ' + pos.coords.latitude.toFixed(5) + ', ' + pos.coords.longitude.toFixed(5);
      field.value = field.value ? field.value + '\n' + point : point;
      setMsg('msg', '');
    }, () => {
      setMsg('msg', 'Could not get your location. Allow location access or type the address instead.', 'error');
    });
  });

  document.getElementById('form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn = document.getElementById('submit');
    btn.disabled = true;
    setMsg('msg', '');
    try {
      await api('/api/bookings', 'POST', {
        patientName: document.getElementById('patientName').value,
        pickupAddress: document.getElementById('pickupAddress').value,
        hospitalId: select.value ? Number(select.value) : null,
        emergencyDetails: document.getElementById('emergencyDetails').value
      });
      location.href = 'my-bookings.html?new=1';
    } catch (err) {
      setMsg('msg', err.message, 'error');
      btn.disabled = false;
    }
  });
})();
