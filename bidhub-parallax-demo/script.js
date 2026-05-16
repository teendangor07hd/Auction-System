let currentBidAmount = 1250;

function placeBid() {
    const btn = document.querySelector('.btn-bid');
    
    // Disable button temporarily to simulate network request
    btn.disabled = true;
    btn.innerHTML = "Processing...";
    btn.style.opacity = "0.7";

    setTimeout(() => {
        // Increase bid
        currentBidAmount += 50;
        
        // Update UI
        const bidElement = document.getElementById('currentBid');
        bidElement.innerHTML = `$${currentBidAmount.toLocaleString()}`;
        
        // Add a pulse animation class (defined in CSS or inline here)
        bidElement.style.transition = "transform 0.2s, color 0.2s";
        bidElement.style.transform = "scale(1.2)";
        bidElement.style.color = "#ff8c00";
        bidElement.style.textShadow = "0 0 10px #ff8c00";

        setTimeout(() => {
            bidElement.style.transform = "scale(1)";
            bidElement.style.color = "white";
            bidElement.style.textShadow = "none";
        }, 300);

        // Reset button
        btn.disabled = false;
        btn.innerHTML = "Place Bid (+ $50)";
        btn.style.opacity = "1";

    }, 800); // 800ms fake delay
}

// Parallax scroll listener for bird animation (Optional enhancement)
const container = document.querySelector('.parallax-container');
const birds = document.querySelectorAll('.bird');

container.addEventListener('scroll', () => {
    const scrolled = container.scrollTop;
    
    // Move birds slightly horizontally as we scroll
    birds[0].style.transform = `translateX(${scrolled * 0.1}px)`;
    birds[1].style.transform = `scale(0.8) translateX(${scrolled * 0.15}px)`;
    birds[2].style.transform = `scale(0.6) translateX(${scrolled * 0.08}px)`;
});
