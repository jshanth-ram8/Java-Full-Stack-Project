/* Login and register pages */

async function redirectIfLoggedIn() {
  const user = await loadUser();
  renderNav(user);
  if (user) location.href = HOME[user.role];
}

function initLogin() {
  redirectIfLoggedIn();
  document.getElementById('form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn = document.getElementById('submit');
    btn.disabled = true;
    setMsg('msg', '');
    try {
      const user = await api('/api/auth/login', 'POST', {
        email: document.getElementById('email').value,
        password: document.getElementById('password').value
      });
      location.href = HOME[user.role];
    } catch (err) {
      setMsg('msg', err.message, 'error');
      btn.disabled = false;
    }
  });
}

function initRegister() {
  redirectIfLoggedIn();
  document.getElementById('form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn = document.getElementById('submit');
    btn.disabled = true;
    setMsg('msg', '');
    try {
      const user = await api('/api/auth/register', 'POST', {
        name: document.getElementById('fullName').value,
        email: document.getElementById('email').value,
        phone: document.getElementById('phone').value,
        password: document.getElementById('password').value
      });
      location.href = HOME[user.role];
    } catch (err) {
      setMsg('msg', err.message, 'error');
      btn.disabled = false;
    }
  });
}
