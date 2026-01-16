/**
 * Invoice Form - Dynamic form handling
 */

/**
 * Calculates and updates all totals in the invoice form.
 */
function calculateTotals() {
    let totalNet = 0;
    let totalVat = 0;
    
    const rows = document.querySelectorAll('#itemsBody tr');
    
    rows.forEach(function(row, index) {
        const qtyInput = row.querySelector('.item-quantity');
        const priceInput = row.querySelector('.item-price');
        const vatSelect = row.querySelector('.item-vat');
        const netDisplay = document.getElementById('item-net-' + index);
        
        if (qtyInput && priceInput && vatSelect) {
            const qty = parseFloat(qtyInput.value) || 0;
            const price = parseFloat(priceInput.value) || 0;
            const vatRate = parseFloat(vatSelect.value) || 0;
            
            const net = qty * price;
            const vat = net * vatRate / 100;
            
            if (netDisplay) {
                netDisplay.value = net.toFixed(2);
            }
            
            totalNet += net;
            totalVat += vat;
        }
    });
    
    // Update totals display
    const totalNetEl = document.getElementById('totalNet');
    const totalVatEl = document.getElementById('totalVat');
    const totalGrossEl = document.getElementById('totalGross');
    
    if (totalNetEl) totalNetEl.textContent = totalNet.toFixed(2);
    if (totalVatEl) totalVatEl.textContent = totalVat.toFixed(2);
    if (totalGrossEl) totalGrossEl.textContent = (totalNet + totalVat).toFixed(2);
}

/**
 * Formats a number as currency.
 * @param {number} value - The value to format
 * @param {string} currency - Currency code (default: EUR)
 * @returns {string} Formatted currency string
 */
function formatCurrency(value, currency = 'EUR') {
    return new Intl.NumberFormat('de-DE', {
        style: 'currency',
        currency: currency
    }).format(value);
}

/**
 * Validates IBAN format.
 * @param {string} iban - The IBAN to validate
 * @returns {boolean} True if valid format
 */
function validateIBAN(iban) {
    const cleaned = iban.replace(/\s+/g, '').toUpperCase();
    const regex = /^[A-Z]{2}[0-9]{2}[A-Z0-9]{4,30}$/;
    return regex.test(cleaned);
}

/**
 * Validates BIC format.
 * @param {string} bic - The BIC to validate
 * @returns {boolean} True if valid format
 */
function validateBIC(bic) {
    if (!bic) return true; // BIC is optional
    const cleaned = bic.replace(/\s+/g, '').toUpperCase();
    const regex = /^[A-Z]{6}[A-Z0-9]{2,5}$/;
    return regex.test(cleaned);
}

/**
 * Formats IBAN with spaces every 4 characters.
 * @param {HTMLInputElement} input - The input element
 */
function formatIBANInput(input) {
    let value = input.value.replace(/\s+/g, '').toUpperCase();
    let formatted = value.replace(/(.{4})/g, '$1 ').trim();
    input.value = formatted;
}

/**
 * Auto-calculates due date based on issue date and payment terms.
 * @param {number} days - Number of days to add
 */
function setDueDate(days) {
    const issueDateInput = document.querySelector('input[name="issueDate"]');
    const dueDateInput = document.querySelector('input[name="dueDate"]');
    
    if (issueDateInput && dueDateInput && issueDateInput.value) {
        const issueDate = new Date(issueDateInput.value);
        issueDate.setDate(issueDate.getDate() + days);
        dueDateInput.value = issueDate.toISOString().split('T')[0];
    }
}

/**
 * Initializes form event listeners.
 */
document.addEventListener('DOMContentLoaded', function() {
    // Calculate totals on page load
    calculateTotals();
    
    // Add event listeners for quantity, price, and VAT changes
    document.querySelectorAll('.item-quantity, .item-price, .item-vat').forEach(function(el) {
        el.addEventListener('change', calculateTotals);
        el.addEventListener('input', calculateTotals);
    });
    
    // IBAN formatting
    const ibanInput = document.querySelector('input[name="bankIban"]');
    if (ibanInput) {
        ibanInput.addEventListener('blur', function() {
            formatIBANInput(this);
        });
        
        ibanInput.addEventListener('input', function() {
            // Remove invalid characters
            this.value = this.value.replace(/[^A-Za-z0-9\s]/g, '').toUpperCase();
        });
    }
    
    // BIC formatting
    const bicInput = document.querySelector('input[name="bankBic"]');
    if (bicInput) {
        bicInput.addEventListener('input', function() {
            this.value = this.value.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
        });
    }
    
    // Issue date change - auto-update due date
    const issueDateInput = document.querySelector('input[name="issueDate"]');
    if (issueDateInput) {
        issueDateInput.addEventListener('change', function() {
            const dueDateInput = document.querySelector('input[name="dueDate"]');
            if (dueDateInput && !dueDateInput.value) {
                setDueDate(30); // Default: 30 days
            }
        });
    }
    
    // Form validation before submit
    const form = document.getElementById('invoiceForm');
    if (form) {
        form.addEventListener('submit', function(e) {
            // Check if at least one item exists
            const items = document.querySelectorAll('#itemsBody tr');
            if (items.length === 0) {
                e.preventDefault();
                alert('Bitte fügen Sie mindestens eine Rechnungsposition hinzu.');
                return false;
            }
            
            // Validate IBAN if provided
            const ibanInput = document.querySelector('input[name="bankIban"]');
            if (ibanInput && ibanInput.value && !validateIBAN(ibanInput.value)) {
                e.preventDefault();
                alert('Bitte geben Sie eine gültige IBAN ein.');
                ibanInput.focus();
                return false;
            }
            
            // Validate BIC if provided
            const bicInput = document.querySelector('input[name="bankBic"]');
            if (bicInput && bicInput.value && !validateBIC(bicInput.value)) {
                e.preventDefault();
                alert('Bitte geben Sie einen gültigen BIC ein.');
                bicInput.focus();
                return false;
            }
        });
    }
    
    // Number input formatting
    document.querySelectorAll('input[type="number"]').forEach(function(input) {
        input.addEventListener('wheel', function(e) {
            // Prevent scroll from changing number inputs
            e.preventDefault();
        });
    });
});

/**
 * Copies seller data to buyer (convenience function).
 */
function copySellerToBuyer() {
    const fields = ['Name', 'Street', 'PostalCode', 'City', 'CountryCode', 'VatId', 'Email', 'Phone'];
    
    fields.forEach(function(field) {
        const sellerInput = document.querySelector('[name="seller' + field + '"]');
        const buyerInput = document.querySelector('[name="buyer' + field + '"]');
        
        if (sellerInput && buyerInput) {
            buyerInput.value = sellerInput.value;
        }
    });
}

/**
 * Clears all form fields in a section.
 * @param {string} prefix - Field name prefix ('seller' or 'buyer')
 */
function clearSection(prefix) {
    document.querySelectorAll('[name^="' + prefix + '"]').forEach(function(input) {
        if (input.tagName === 'SELECT') {
            input.selectedIndex = 0;
        } else {
            input.value = '';
        }
    });
}
