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

let authToken = null;
let optionCount = 0;

document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    setupEventListeners();
    addOptionField();
    addOptionField();
});

function checkAuth() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        window.location.href = 'login.html';
        return;
    }
    authToken = token;
}

function setupEventListeners() {
    document.getElementById('add-option-btn').addEventListener('click', () => addOptionField());
    
    document.getElementById('save-draft-btn').addEventListener('click', () => saveVote(false));
    
    document.getElementById('publish-btn').addEventListener('click', () => saveVote(true));
    
    document.getElementById('cancel-btn').addEventListener('click', () => {
        if (confirm('Are you sure you want to cancel? Unsaved changes will be lost.')) {
            window.location.href = 'index.html';
        }
    });
    
    document.getElementById('vote-description').addEventListener('input', (e) => {
        document.getElementById('description-count').textContent = e.target.value.length;
    });
    
    document.getElementById('vote-title').addEventListener('input', updatePreview);
    document.getElementById('vote-description').addEventListener('input', updatePreview);
    
    document.getElementById('vote-deadline').addEventListener('change', updatePreview);
    
    document.getElementById('multiple-choice').addEventListener('change', (e) => {
        const label = document.getElementById('multiple-label');
        const toggle = e.target.nextElementSibling;
        label.textContent = e.target.checked ? 'On' : 'Off';
        if (e.target.checked) {
            toggle.classList.add('bg-blue-600');
            toggle.classList.remove('bg-gray-200');
            toggle.querySelector('div').classList.add('translate-x-6');
        } else {
            toggle.classList.remove('bg-blue-600');
            toggle.classList.add('bg-gray-200');
            toggle.querySelector('div').classList.remove('translate-x-6');
        }
        updatePreview();
    });
    
    document.getElementById('anonymous-vote').addEventListener('change', (e) => {
        const label = document.getElementById('anonymous-label');
        const toggle = e.target.nextElementSibling;
        label.textContent = e.target.checked ? 'On' : 'Off';
        if (e.target.checked) {
            toggle.classList.add('bg-blue-600');
            toggle.classList.remove('bg-gray-200');
            toggle.querySelector('div').classList.add('translate-x-6');
        } else {
            toggle.classList.remove('bg-blue-600');
            toggle.classList.add('bg-gray-200');
            toggle.querySelector('div').classList.remove('translate-x-6');
        }
    });
    
    document.querySelectorAll('input[name="permission"]').forEach(radio => {
        radio.addEventListener('change', (e) => {
            document.querySelectorAll('.permission-option').forEach(option => {
                option.classList.remove('border-blue-500', 'bg-blue-50');
                option.classList.add('border-gray-300');
            });
            e.target.closest('.permission-option').classList.remove('border-gray-300');
            e.target.closest('.permission-option').classList.add('border-blue-500', 'bg-blue-50');
            updatePreview();
        });
    });
}

function addOptionField(value = '') {
    optionCount++;
    const container = document.getElementById('options-container');
    const optionDiv = document.createElement('div');
    optionDiv.className = 'flex items-center';
    optionDiv.innerHTML = `
        <input type="text" class="option-input flex-grow px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
               placeholder="Option Title" value="${value}" required/>
        <button type="button" class="ml-3 text-gray-500 hover:text-red-500 delete-option" onclick="this.parentElement.remove(); updatePreview();">
            <iconify-icon icon="mdi:delete-outline" class="text-2xl"></iconify-icon>
        </button>
    `;
    container.appendChild(optionDiv);
    
    optionDiv.querySelector('.option-input').addEventListener('input', updatePreview);
    
    updatePreview();
}

function updatePreview() {
    const title = document.getElementById('vote-title').value || 'Enter vote title...';
    const description = document.getElementById('vote-description').value || 'Enter vote description...';
    const deadline = document.getElementById('vote-deadline').value;
    const multipleChoice = document.getElementById('multiple-choice').checked;
    
    document.getElementById('preview-title').textContent = title;
    document.getElementById('preview-title').className = title === 'Enter vote title...' ? 'text-xl font-bold mb-4 text-gray-400' : 'text-xl font-bold mb-4';
    document.getElementById('preview-description').textContent = description;
    document.getElementById('preview-description').className = description === 'Enter vote description...' ? 'text-gray-400 mb-6' : 'text-gray-600 mb-6';
    
    if (deadline) {
        const date = new Date(deadline);
        document.getElementById('preview-deadline').textContent = `Deadline: ${date.toLocaleDateString()} ${date.toLocaleTimeString()}`;
    } else {
        document.getElementById('preview-deadline').textContent = 'No deadline set';
    }
    
    document.getElementById('preview-type').textContent = multipleChoice ? 'Multiple Choice Vote' : 'Single Choice Vote';
    
    const options = Array.from(document.querySelectorAll('.option-input')).map(input => input.value.trim()).filter(val => val !== '');
    const previewContainer = document.getElementById('preview-options');
    
    if (options.length === 0) {
        previewContainer.innerHTML = '<p class="text-gray-400 text-sm">Add options to see preview</p>';
    } else {
        previewContainer.innerHTML = options.map((option, index) => `
            <div class="p-4 border border-gray-200 rounded-lg flex items-center">
                <input class="hidden" id="preview-option${index}" name="votePreview" type="${multipleChoice ? 'checkbox' : 'radio'}"/>
                <label class="flex items-center cursor-pointer" for="preview-option${index}">
                    <div class="w-6 h-6 ${multipleChoice ? 'rounded' : 'rounded-full'} border-2 border-gray-300 mr-3 flex-shrink-0"></div>
                    <span>${option || 'Option ' + (index + 1)}</span>
                </label>
            </div>
        `).join('');
    }
}

async function saveVote(publish = false) {
    const title = document.getElementById('vote-title').value.trim();
    const description = document.getElementById('vote-description').value.trim();
    const permission = document.querySelector('input[name="permission"]:checked').value;
    
    const options = Array.from(document.querySelectorAll('.option-input'))
        .map(input => input.value.trim())
        .filter(val => val !== '');
    
    if (!title) {
        alert('Please enter a vote title');
        return;
    }
    
    if (options.length < 2) {
        alert('At least 2 options are required');
        return;
    }
    
    const voteData = {
        title,
        description,
        options,
        permission
    };
    
    try {
        console.log('Saving vote with data:', voteData);
        console.log('API URL:', `${API_BASE_URL}/votes`);
        
        const response = await fetch(`${API_BASE_URL}/votes`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify(voteData)
        });
        
        console.log('Response status:', response.status, response.statusText);
        console.log('Response headers:', response.headers);
        
        if (!response.ok) {
            let errorMessage = `HTTP ${response.status}: ${response.statusText}`;
            
            try {
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    const errorData = await response.json();
                    console.error('Error response data:', errorData);
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    } else if (errorData.error) {
                        errorMessage = errorData.error;
                    } else if (errorData.errors) {
                        const validationErrors = Object.entries(errorData.errors)
                            .map(([field, msg]) => `${field}: ${msg}`)
                            .join(', ');
                        errorMessage = validationErrors || errorData.message || 'Validation failed';
                    } else if (typeof errorData === 'string') {
                        errorMessage = errorData;
                    } else {
                        const errorValues = Object.values(errorData).filter(v => v != null);
                        if (errorValues.length > 0) {
                            errorMessage = errorValues.join(', ');
                        } else {
                            errorMessage = JSON.stringify(errorData);
                        }
                    }
                } else {
                    const text = await response.text();
                    console.error('Error response text:', text);
                    if (text && text.trim()) {
                        errorMessage = text;
                    }
                }
            } catch (e) {
                console.error('Error parsing error response:', e);
                errorMessage = `HTTP ${response.status}: ${response.statusText}`;
            }
            
            alert('Error saving vote: ' + errorMessage);
            return;
        }
        
        const contentType = response.headers.get('content-type');
        let vote;
        
        const responseText = await response.text();
        
        if (!responseText || responseText.trim() === '') {
            if (publish) {
                alert('Vote created and published successfully!');
            } else {
                alert('Vote saved as draft!');
            }
            window.location.href = 'index.html';
            return;
        }
        
        if (contentType && contentType.includes('application/json')) {
            try {
                vote = JSON.parse(responseText);
            } catch (jsonError) {
                console.error('Error parsing JSON response:', jsonError);
                console.error('Response text:', responseText);
                if (publish) {
                    alert('Vote created and published successfully!');
                } else {
                    alert('Vote saved as draft!');
                }
                window.location.href = 'index.html';
                return;
            }
        } else {
            if (publish) {
                alert('Vote created and published successfully!');
            } else {
                alert('Vote saved as draft!');
            }
            window.location.href = 'index.html';
            return;
        }
        
        if (publish) {
            const publishResponse = await fetch(`${API_BASE_URL}/votes/${vote.id}/publish`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${authToken}`
                }
            });
            
            if (!publishResponse.ok) {
                const publishContentType = publishResponse.headers.get('content-type');
                let publishErrorMessage = `HTTP ${publishResponse.status}: ${publishResponse.statusText}`;
                
                try {
                    if (publishContentType && publishContentType.includes('application/json')) {
                        const publishErrorData = await publishResponse.json();
                        publishErrorMessage = publishErrorData.message || publishErrorData.error || publishErrorMessage;
                    } else {
                        const text = await publishResponse.text();
                        if (text) {
                            publishErrorMessage = text;
                        }
                    }
                } catch (e) {
                    console.error('Error parsing publish response:', e);
                }
                
                alert('Vote created but failed to publish: ' + publishErrorMessage);
                window.location.href = 'index.html';
                return;
            }
            
            try {
                await publishResponse.json();
            } catch (e) {
            }
            
            alert('Vote created and published successfully!');
            window.location.href = 'index.html';
        } else {
            alert('Vote saved as draft!');
            window.location.href = 'index.html';
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

