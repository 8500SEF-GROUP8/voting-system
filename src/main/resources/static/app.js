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

let currentUser = null;
let authToken = null;

document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    setupEventListeners();
});

function checkAuth() {
    const token = localStorage.getItem('authToken');
    const user = localStorage.getItem('currentUser');
    
    if (token && user) {
        authToken = token;
        currentUser = JSON.parse(user);
        showMainSection();
        loadVotes();
    } else {
        window.location.href = 'login.html';
    }
}

function setupEventListeners() {
    document.getElementById('logout-btn').addEventListener('click', handleLogout);
    
    document.getElementById('create-vote-btn').addEventListener('click', () => {
        window.location.href = 'create-vote.html';
    });
    
    document.getElementById('status-filter').addEventListener('change', loadVotes);
    
    document.querySelectorAll('.close').forEach(close => {
        close.addEventListener('click', (e) => {
            if (e.target.closest('#vote-modal')) {
                closeModal();
            } else {
                closeVoteDetailModal();
            }
        });
    });
    
    document.getElementById('vote-form').addEventListener('submit', handleSaveVote);
    
    document.getElementById('add-option-btn').addEventListener('click', addOptionField);
    
    document.getElementById('publish-btn').addEventListener('click', handlePublishVote);
}

function showMainSection() {
    document.getElementById('user-info').textContent = `Welcome, ${currentUser.username}`;
}

function handleLogout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('currentUser');
    authToken = null;
    currentUser = null;
    window.location.href = 'login.html';
}

async function loadVotes() {
    try {
        const status = document.getElementById('status-filter').value;
        const url = status ? `${API_BASE_URL}/votes?status=${status}` : `${API_BASE_URL}/votes`;
        
        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            const votes = await response.json();
            displayVotes(votes);
        }
    } catch (error) {
        console.error('Error loading votes:', error);
    }
}

function displayVotes(votes) {
    const voteList = document.getElementById('vote-list');
    voteList.innerHTML = '';
    
    if (votes.length === 0) {
        voteList.innerHTML = '<p class="text-center text-gray-600 text-lg col-span-full py-8">No votes found</p>';
        return;
    }
    
    votes.forEach(vote => {
        const card = createVoteCard(vote);
        voteList.appendChild(card);
    });
}

function createVoteCard(vote) {
    const card = document.createElement('div');
    card.className = 'bg-white border border-gray-200 rounded-xl shadow-md p-6 hover:shadow-lg transition cursor-pointer';
    card.onclick = () => openVoteDetail(vote.id);
    
    const statusColors = {
        'draft': 'bg-yellow-100 text-yellow-800',
        'published': 'bg-green-100 text-green-800',
        'closed': 'bg-gray-100 text-gray-800'
    };
    const statusColor = statusColors[vote.status.toLowerCase()] || 'bg-gray-100 text-gray-800';
    const isOwner = vote.creatorId === currentUser.id;
    
    card.innerHTML = `
        <div class="flex justify-between items-start mb-4">
            <h3 class="text-lg font-semibold text-gray-800 flex-1">${vote.title}</h3>
            <span class="px-3 py-1 rounded-full text-xs font-medium ${statusColor} ml-2">${vote.status}</span>
        </div>
        <p class="text-gray-600 text-sm mb-4 line-clamp-2">${vote.description || 'No description'}</p>
        <div class="flex justify-between items-center text-sm text-gray-500 mb-4">
            <span>By ${vote.creatorUsername}</span>
            <span>${vote.totalVotes || 0} votes</span>
        </div>
        <div class="flex flex-wrap gap-2">
            ${isOwner && vote.status !== 'DELETED' && vote.status !== 'CLOSED' ? `
                <button class="bg-gray-200 hover:bg-gray-300 text-gray-800 px-3 py-1 rounded-lg text-sm font-medium transition" onclick="event.stopPropagation(); editVote(${vote.id})">Edit</button>
                ${vote.status === 'DRAFT' ? `<button class="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded-lg text-sm font-medium transition" onclick="event.stopPropagation(); publishVote(${vote.id})">Publish</button>` : ''}
                ${vote.status === 'PUBLISHED' ? `<button class="bg-gray-200 hover:bg-gray-300 text-gray-800 px-3 py-1 rounded-lg text-sm font-medium transition" onclick="event.stopPropagation(); closeVote(${vote.id})">Close</button>` : ''}
            ` : ''}
            ${isOwner && vote.status !== 'DELETED' ? `
                <button class="bg-red-600 hover:bg-red-700 text-white px-3 py-1 rounded-lg text-sm font-medium transition" onclick="event.stopPropagation(); deleteVote(${vote.id})">Delete</button>
            ` : ''}
            <button class="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded-lg text-sm font-medium transition" onclick="event.stopPropagation(); openVoteDetail(${vote.id})">View</button>
        </div>
    `;
    
    return card;
}

function openVoteModal(vote = null) {
    const modal = document.getElementById('vote-modal');
    const form = document.getElementById('vote-form');
    const title = document.getElementById('modal-title');
    const publishBtn = document.getElementById('publish-btn');
    
    if (vote) {
        title.textContent = 'Edit Vote';
        document.getElementById('vote-id').value = vote.id;
        document.getElementById('vote-title').value = vote.title;
        document.getElementById('vote-description').value = vote.description || '';
        document.getElementById('vote-permission').value = vote.permission;
        
        const optionsContainer = document.getElementById('options-container');
        optionsContainer.innerHTML = '';
        vote.options.forEach(option => {
            addOptionField(option.text);
        });
        
        if (vote.status === 'DRAFT') {
            publishBtn.classList.remove('hidden');
        } else {
            publishBtn.classList.add('hidden');
        }
    } else {
        title.textContent = 'Create Vote';
        form.reset();
        document.getElementById('vote-id').value = '';
        document.getElementById('options-container').innerHTML = '';
        addOptionField();
        addOptionField();
        publishBtn.classList.add('hidden');
    }
    
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

function closeModal() {
    const modal = document.getElementById('vote-modal');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
    document.getElementById('vote-form').reset();
}

function addOptionField(value = '') {
    const container = document.getElementById('options-container');
    const optionDiv = document.createElement('div');
    optionDiv.className = 'flex gap-2';
    optionDiv.innerHTML = `
        <input type="text" class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent" value="${value}" placeholder="Option text" required>
        <button type="button" class="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg font-medium transition" onclick="this.parentElement.remove()">Remove</button>
    `;
    container.appendChild(optionDiv);
}

async function handleSaveVote(e) {
    e.preventDefault();
    
    const voteId = document.getElementById('vote-id').value;
    const title = document.getElementById('vote-title').value;
    const description = document.getElementById('vote-description').value;
    const permission = document.getElementById('vote-permission').value;
    
    const options = Array.from(document.querySelectorAll('.option-input'))
        .map(input => input.value.trim())
        .filter(val => val !== '');
    
    if (options.length < 2) {
        alert('At least 2 options are required');
        return;
    }
    
    const voteData = { title, description, options, permission };
    
    try {
        const url = voteId ? `${API_BASE_URL}/votes/${voteId}` : `${API_BASE_URL}/votes`;
        const method = voteId ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify(voteData)
        });
        
        if (response.ok) {
            closeModal();
            loadVotes();
        } else {
            const data = await response.json();
            alert(data.message || 'Error saving vote');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function handlePublishVote() {
    const voteId = document.getElementById('vote-id').value;
    if (!voteId) {
        alert('Please save the vote first');
        return;
    }
    
    await publishVote(voteId);
}

async function publishVote(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/votes/${id}/publish`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            loadVotes();
            const modal = document.getElementById('vote-modal');
            if (!modal.classList.contains('hidden')) {
                closeModal();
            }
        } else {
            const data = await response.json();
            alert(data.message || 'Error publishing vote');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function editVote(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/votes/${id}`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            const vote = await response.json();
            openVoteModal(vote);
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function closeVote(id) {
    if (!confirm('Are you sure you want to close this vote?')) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/votes/${id}/close`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            loadVotes();
        } else {
            const data = await response.json();
            alert(data.message || 'Error closing vote');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function deleteVote(id) {
    if (!confirm('Are you sure you want to delete this vote?')) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/votes/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            loadVotes();
        } else {
            const data = await response.json();
            alert(data.message || 'Error deleting vote');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function openVoteDetail(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/votes/${id}`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            const vote = await response.json();
            displayVoteDetail(vote);
        } else {
            alert('Error loading vote details');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

function displayVoteDetail(vote) {
    const modal = document.getElementById('vote-detail-modal');
    const content = document.getElementById('vote-detail-content');
    
    const isOwner = vote.creatorId === currentUser.id;
    const canVote = vote.status === 'PUBLISHED' && !vote.hasVoted && !vote.closedAt;
    const showResults = vote.hasVoted || vote.closedAt || isOwner;
    
    const shareLink = `${window.location.origin}${window.location.pathname}?share=${vote.shareToken}`;
    
    content.innerHTML = `
        <div class="space-y-6">
            <div class="border-b border-gray-200 pb-4">
                <h2 class="text-2xl font-semibold text-gray-800 mb-2">${vote.title}</h2>
                <p class="text-gray-600 mb-4">${vote.description || 'No description'}</p>
                <div class="flex flex-wrap gap-4 text-sm text-gray-500">
                    <span>Created by: ${vote.creatorUsername}</span>
                    <span>Status: ${vote.status}</span>
                    <span>Total votes: ${vote.totalVotes || 0}</span>
                </div>
            </div>
            
            <div class="space-y-3">
                ${vote.options.map((option, index) => `
                    <div class="bg-gray-50 border-2 border-transparent rounded-lg p-4 transition ${canVote ? 'hover:border-blue-500 cursor-pointer' : ''}" 
                         data-option-id="${option.id}" 
                         ${canVote ? 'onclick="selectOption(' + option.id + ')"' : ''}>
                        <div class="font-medium text-gray-800 mb-2">${option.text}</div>
                        ${showResults ? `
                            <div class="mt-3">
                                <div class="w-full bg-gray-200 rounded-full h-6 mb-2 relative overflow-hidden">
                                    <div class="bg-gradient-to-r from-blue-500 to-purple-600 h-6 rounded-full flex items-center justify-center text-white text-xs font-medium transition-all" style="width: ${option.percentage || 0}%">
                                        ${option.percentage && option.percentage > 5 ? option.percentage.toFixed(1) + '%' : ''}
                                    </div>
                                </div>
                                <div class="text-sm text-gray-600">${option.voteCount || 0} votes ${option.percentage ? '(' + option.percentage.toFixed(1) + '%)' : ''}</div>
                            </div>
                        ` : ''}
                    </div>
                `).join('')}
            </div>
            
            ${canVote ? `
                <button class="w-full bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-lg font-semibold transition" id="submit-vote-btn" data-vote-id="${vote.id}">
                    Submit Vote
                </button>
            ` : ''}
            
            ${isOwner ? `
                <div class="bg-gray-50 border border-gray-200 rounded-lg p-6">
                    <h3 class="font-semibold text-gray-800 mb-2">Share Vote</h3>
                    <p class="text-sm text-gray-600 mb-4">Share this link to allow others to participate:</p>
                    <div class="flex gap-2 mb-4">
                        <input type="text" id="share-link-input" value="${shareLink}" readonly class="flex-1 px-4 py-2 border border-gray-300 rounded-lg bg-white">
                        <button class="bg-gray-200 hover:bg-gray-300 text-gray-800 px-4 py-2 rounded-lg font-medium transition" onclick="copyShareLink()">Copy</button>
                    </div>
                    <div>
                        <label class="block text-sm font-medium mb-2">Permission:</label>
                        <select id="permission-select" onchange="updatePermission(${vote.id})" class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                            <option value="PUBLIC" ${vote.permission === 'PUBLIC' ? 'selected' : ''}>Public</option>
                            <option value="PRIVATE" ${vote.permission === 'PRIVATE' ? 'selected' : ''}>Private</option>
                            <option value="LINK_ONLY" ${vote.permission === 'LINK_ONLY' ? 'selected' : ''}>Link Only</option>
                        </select>
                    </div>
                </div>
            ` : ''}
        </div>
    `;
    
    modal.classList.remove('hidden');
    modal.classList.add('flex');
    selectedOptionId = null;
    
    const submitBtn = document.getElementById('submit-vote-btn');
    if (submitBtn) {
        submitBtn.onclick = () => {
            const voteId = parseInt(submitBtn.getAttribute('data-vote-id'));
            submitVote(voteId);
        };
    }
}

function closeVoteDetailModal() {
    const modal = document.getElementById('vote-detail-modal');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

let selectedOptionId = null;
function selectOption(optionId) {
    selectedOptionId = optionId;
    document.querySelectorAll('[data-option-id]').forEach(card => {
        card.classList.remove('border-blue-500', 'bg-blue-50');
        card.classList.add('border-transparent');
    });
    const selectedCard = document.querySelector(`[data-option-id="${optionId}"]`);
    if (selectedCard) {
        selectedCard.classList.remove('border-transparent');
        selectedCard.classList.add('border-blue-500', 'bg-blue-50');
    }
}

async function submitVote(voteId) {
    if (!selectedOptionId) {
        alert('Please select an option');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/votes/${voteId}/participate?optionId=${selectedOptionId}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        const contentType = response.headers.get('content-type');
        const responseText = await response.text();
        
        if (response.ok) {
            openVoteDetail(voteId);
            loadVotes();
        } else {
            let errorMessage = `HTTP ${response.status}: ${response.statusText}`;
            
            if (responseText && responseText.trim() !== '') {
                if (contentType && contentType.includes('application/json')) {
                    try {
                        const errorData = JSON.parse(responseText);
                        errorMessage = errorData.message || errorData.error || errorMessage;
                    } catch (e) {
                        // Not JSON, use text as is
                        errorMessage = responseText;
                    }
                } else {
                    errorMessage = responseText;
                }
            }
            
            alert('Error submitting vote: ' + errorMessage);
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

function copyShareLink() {
    const input = document.getElementById('share-link-input');
    input.select();
    document.execCommand('copy');
    alert('Link copied to clipboard!');
}

async function updatePermission(voteId) {
    const permission = document.getElementById('permission-select').value;
    
    try {
        const response = await fetch(`${API_BASE_URL}/votes/${voteId}/permission?permission=${permission}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            openVoteDetail(voteId);
        } else {
            const data = await response.json();
            alert(data.message || 'Error updating permission');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

window.addEventListener('load', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const shareToken = urlParams.get('share');
    
    if (shareToken) {
        fetch(`${API_BASE_URL}/votes/share/${shareToken}`)
            .then(async response => {
                const contentType = response.headers.get('content-type');
                const responseText = await response.text();
                
                if (response.ok) {
                    if (responseText && responseText.trim() !== '') {
                        if (contentType && contentType.includes('application/json')) {
                            try {
                                const vote = JSON.parse(responseText);
                                displayVoteDetailForShare(vote, shareToken);
                            } catch (jsonError) {
                                console.error('Error parsing JSON response:', jsonError);
                                alert('Invalid share link: Unable to parse response');
                            }
                        } else {
                            alert('Invalid share link: Unexpected response format');
                        }
                    } else {
                        alert('Invalid share link: Empty response');
                    }
                } else {
                    let errorMessage = 'Invalid share link';
                    
                    if (responseText && responseText.trim() !== '') {
                        if (contentType && contentType.includes('application/json')) {
                            try {
                                const errorData = JSON.parse(responseText);
                                errorMessage = errorData.message || errorData.error || errorMessage;
                            } catch (e) {
                                errorMessage = responseText;
                            }
                        } else {
                            errorMessage = responseText;
                        }
                    }
                    
                    alert(errorMessage);
                }
            })
            .catch(error => {
                console.error('Error loading share link:', error);
                alert('Invalid share link: ' + error.message);
            });
    }
});

// Display vote detail for share link
function displayVoteDetailForShare(vote, shareToken) {
    const modal = document.getElementById('vote-detail-modal');
    const content = document.getElementById('vote-detail-content');
    
    const canVote = vote.status === 'PUBLISHED' && !vote.closedAt;
    const showResults = vote.closedAt;
    
    content.innerHTML = `
        <div class="space-y-6">
            <div class="border-b border-gray-200 pb-4">
                <h2 class="text-2xl font-semibold text-gray-800 mb-2">${vote.title}</h2>
                <p class="text-gray-600 mb-4">${vote.description || 'No description'}</p>
                <div class="flex flex-wrap gap-4 text-sm text-gray-500">
                    <span>Created by: ${vote.creatorUsername}</span>
                    <span>Total votes: ${vote.totalVotes || 0}</span>
                </div>
            </div>
            
            <div class="space-y-3">
                ${vote.options.map((option, index) => `
                    <div class="bg-gray-50 border-2 border-transparent rounded-lg p-4 transition ${canVote ? 'hover:border-blue-500 cursor-pointer' : ''}" 
                         data-option-id="${option.id}" 
                         ${canVote ? 'onclick="selectOption(' + option.id + ')"' : ''}>
                        <div class="font-medium text-gray-800 mb-2">${option.text}</div>
                        ${showResults ? `
                            <div class="mt-3">
                                <div class="w-full bg-gray-200 rounded-full h-6 mb-2 relative overflow-hidden">
                                    <div class="bg-gradient-to-r from-blue-500 to-purple-600 h-6 rounded-full flex items-center justify-center text-white text-xs font-medium transition-all" style="width: ${option.percentage || 0}%">
                                        ${option.percentage && option.percentage > 5 ? option.percentage.toFixed(1) + '%' : ''}
                                    </div>
                                </div>
                                <div class="text-sm text-gray-600">${option.voteCount || 0} votes ${option.percentage ? '(' + option.percentage.toFixed(1) + '%)' : ''}</div>
                            </div>
                        ` : ''}
                    </div>
                `).join('')}
            </div>
            
            ${canVote ? `
                <button class="w-full bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-lg font-semibold transition" id="submit-vote-btn-share" data-vote-id="${vote.id}" data-share-token="${shareToken}">
                    Submit Vote
                </button>
            ` : ''}
        </div>
    `;
    
    modal.classList.remove('hidden');
    modal.classList.add('flex');
    selectedOptionId = null;
    
    const submitBtnShare = document.getElementById('submit-vote-btn-share');
    if (submitBtnShare) {
        submitBtnShare.onclick = () => {
            const voteId = parseInt(submitBtnShare.getAttribute('data-vote-id'));
            const shareToken = submitBtnShare.getAttribute('data-share-token');
            submitVoteByShare(voteId, shareToken);
        };
    }
}

async function submitVoteByShare(voteId, shareToken) {
    if (!selectedOptionId) {
        alert('Please select an option');
        return;
    }
    
    try {
        const token = localStorage.getItem('authToken');
        const headers = {};
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
            console.log('=== Submit Vote by Share Debug ===');
            console.log('Sending with JWT token:', token.substring(0, 20) + '...');
            console.log('Headers:', headers);
        } else {
            console.log('=== Submit Vote by Share Debug ===');
            console.log('No JWT token found - anonymous voting');
        }
        console.log('Vote ID:', voteId);
        console.log('Option ID:', selectedOptionId);
        console.log('Share Token:', shareToken);
        console.log('================================');
        
        const response = await fetch(`${API_BASE_URL}/votes/${voteId}/participate-share?optionId=${selectedOptionId}&token=${shareToken}`, {
            method: 'POST',
            headers: headers
        });
        
        const contentType = response.headers.get('content-type');
        const responseText = await response.text();
        
        if (response.ok) {
            if (responseText && responseText.trim() !== '') {
                if (contentType && contentType.includes('application/json')) {
                    try {
                        const data = JSON.parse(responseText);
                        alert('Vote submitted successfully!');
                        closeVoteDetailModal();
                    } catch (jsonError) {
                        console.error('Error parsing JSON response:', jsonError);
                        alert('Vote submitted successfully!');
                        closeVoteDetailModal();
                    }
                } else {
                    alert('Vote submitted successfully!');
                    closeVoteDetailModal();
                }
            } else {
                alert('Vote submitted successfully!');
                closeVoteDetailModal();
            }
        } else {
            let errorMessage = `HTTP ${response.status}: ${response.statusText}`;
            
            if (responseText && responseText.trim() !== '') {
                if (contentType && contentType.includes('application/json')) {
                    try {
                        const errorData = JSON.parse(responseText);
                        errorMessage = errorData.message || errorData.error || errorMessage;
                    } catch (e) {
                        errorMessage = responseText;
                    }
                } else {
                    errorMessage = responseText;
                }
            }
            
            alert('Error submitting vote: ' + errorMessage);
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

window.openVoteModal = openVoteModal;
window.editVote = editVote;
window.publishVote = publishVote;
window.closeVote = closeVote;
window.deleteVote = deleteVote;
window.openVoteDetail = openVoteDetail;
window.selectOption = selectOption;
window.submitVote = submitVote;
window.copyShareLink = copyShareLink;
window.updatePermission = updatePermission;
window.submitVoteByShare = submitVoteByShare;
window.closeModal = closeModal;
window.closeVoteDetailModal = closeVoteDetailModal;

