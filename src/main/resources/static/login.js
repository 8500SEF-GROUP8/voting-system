const API_BASE_URL = (() => {
    if (window.location.protocol === 'file:') {
        return 'http://localhost:8080/api';
    }
    const origin = window.location.origin;
    if (window.location.port && window.location.port !== '8080') {
        return `${window.location.protocol}//${window.location.hostname}:8080/api`;
    }
    return `${origin}/api`;
})();

document.addEventListener('DOMContentLoaded', () => {
    setupEventListeners();
    
    const token = localStorage.getItem('authToken');
    if (token) {
        window.location.href = 'index.html';
    }
});

function setupEventListeners() {
    document.getElementById('login-form-element').addEventListener('submit', handleLogin);
    
    document.getElementById('register-form-element').addEventListener('submit', handleRegister);
}

async function handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;
    
    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });
        
        const contentType = response.headers.get('content-type');
        let data;
        
        if (contentType && contentType.includes('application/json')) {
            data = await response.json();
        } else {
            const text = await response.text();
            alert(text || 'Login failed');
            return;
        }
        
        if (response.ok) {
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('currentUser', JSON.stringify({ 
                id: data.userId, 
                username: data.username 
            }));
            window.location.href = 'index.html';
        } else {
            alert(data.message || 'Login failed');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const username = document.getElementById('register-username').value;
    const email = document.getElementById('register-email').value;
    const password = document.getElementById('register-password').value;
    
    try {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, email, password })
        });
        
        const contentType = response.headers.get('content-type');
        let data;
        
        if (contentType && contentType.includes('application/json')) {
            data = await response.json();
        } else {
            const text = await response.text();
            alert(text || 'Registration failed');
            return;
        }
        
        if (response.ok) {
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('currentUser', JSON.stringify({ 
                id: data.userId, 
                username: data.username 
            }));
            window.location.href = 'index.html';
        } else {
            alert(data.message || 'Registration failed');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

