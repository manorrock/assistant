function sendMessage() {
    const requestArea = document.getElementById('requestArea');
    const responseArea = document.getElementById('responseArea');
    const logArea = document.getElementById('logArea');
    const progressBar = document.getElementById('progressBar');
    const message = requestArea.value.trim();

    if (message) {
        responseArea.value += `\n\nYou: ${message}`;
        logArea.value += `\n\nYou: ${message}`;
        requestArea.value = '';

        progressBar.value = 50; // Simulate progress

        fetch('/api/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ message })
        })
        .then(response => response.text())
        .then(data => {
            responseArea.value += `\n\nAssistant: ${data}`;
            logArea.value += `\n\nAssistant: ${data}`;
            progressBar.value = 100; // Simulate progress completion
        })
        .catch(error => {
            responseArea.value += `\n\nAssistant: Error processing message`;
            logArea.value += `\n\nError: ${error}`;
            progressBar.value = 0; // Reset progress
        });
    }
}

function startOver() {
    document.getElementById('responseArea').value = 'Welcome to Manorrock Assistant';
    document.getElementById('logArea').value = 'Chat Log\n---------';
    document.getElementById('requestArea').value = '';
    document.getElementById('progressBar').value = 0;
}

function allowDrop(event) {
    event.preventDefault();
}

function handleDrop(event) {
    event.preventDefault();
    const files = event.dataTransfer.files;
    const responseArea = document.getElementById('responseArea');
    const logArea = document.getElementById('logArea');

    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const formData = new FormData();
        formData.append('file', file);

        fetch('/api/chat/upload', {
            method: 'POST',
            body: formData
        })
        .then(response => response.text())
        .then(data => {
            responseArea.value += `\n\nYou dropped: ${file.name}`;
            responseArea.value += `\n\nAssistant: ${data}`;
            logArea.value += `\n\nYou dropped: ${file.name}`;
            logArea.value += `\n\nAssistant: ${data}`;
        })
        .catch(error => {
            responseArea.value += `\n\nAssistant: Error processing file`;
            logArea.value += `\n\nError: ${error}`;
        });
    }
}
