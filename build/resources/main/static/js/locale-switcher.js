/**
 * Locale Switcher - On-the-fly language change
 */

/**
 * Changes the application language via AJAX and reloads the page.
 * @param {string} lang - Language code ('de' or 'en')
 */
function changeLanguage(lang) {
    fetch('/api/locale?lang=' + encodeURIComponent(lang), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (response.ok) {
            // Reload the page to apply the new language
            location.reload();
        } else {
            console.error('Failed to change language:', response.statusText);
        }
    })
    .catch(error => {
        console.error('Error changing language:', error);
        // Fallback: Use URL parameter
        const url = new URL(window.location.href);
        url.searchParams.set('lang', lang);
        window.location.href = url.toString();
    });
}

/**
 * Gets the current language from the page.
 * @returns {string} Current language code
 */
function getCurrentLanguage() {
    return document.documentElement.lang || 'de';
}

/**
 * Initializes language dropdown active state.
 */
document.addEventListener('DOMContentLoaded', function() {
    const currentLang = getCurrentLanguage();
    
    // Mark current language as active in dropdown
    document.querySelectorAll('.dropdown-item').forEach(function(item) {
        const href = item.getAttribute('onclick');
        if (href && href.includes("'" + currentLang + "'")) {
            item.classList.add('active');
        }
    });
});
