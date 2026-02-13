(function (window) {
  "use strict";

  class LeoNewsletterModal {
    constructor(options = {}) {
      this.options = Object.assign(
        {
          locale: "en",
          mountTo: document.body,
          triggerSelector: "[data-leo-newsletter]",

          // Feature Flags
          collectPhone: false,

          // Theme Settings: 'light', 'dark', or 'auto' (detects OS preference)
          theme: "light",

          // Auto-close success message after N ms
          autoCloseDelay: 3000,

          i18n: {},
        },
        options,
      );

      this._injectStyles(); // 1. Inject CSS dynamically
      this._createModal();
      this._bindTriggers();
    }

    // ======================
    // Public API
    // ======================

    open(source = null) {
      this.overlay.classList.add("active");
      this.overlay.style.display = "flex"; // Ensure flex for centering
      this._dispatch("leo.newsletter.open", { source });

      // UX: Focus the first input
      setTimeout(() => {
        if (this.inputName) this.inputName.focus();
      }, 100);
    }

    close() {
      this.overlay.classList.remove("active");

      // Wait for animation to finish before hiding
      setTimeout(() => {
        this.overlay.style.display = "none";
      }, 250);

      this._dispatch("leo.newsletter.close", {});
    }

    // ======================
    // Private: Logic
    // ======================

    _createModal() {
      this.overlay = document.createElement("div");
      this.overlay.className = "leo-modal-overlay";

      // Handle Theme Class
      let themeClass = "leo-theme-light";
      if (this.options.theme === "dark") {
        themeClass = "leo-theme-dark";
      } else if (this.options.theme === "auto") {
        // Check OS preference
        if (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) {
          themeClass = "leo-theme-dark";
        }
      }

      const phoneHtml = this.options.collectPhone
        ? `<input type="tel" id="leo-phone-input" class="leo-input" placeholder="Phone Number" />`
        : "";

      this.overlay.innerHTML = `
        <div class="leo-modal ${themeClass}">
          <button class="leo-modal-close" aria-label="Close">&times;</button>
          <div class="leo-modal-title"></div>
          <div class="leo-modal-description"></div>
          <form>
            <input type="text" id="leo-name-input" class="leo-input" required />
            <input type="email" id="leo-email-input" class="leo-input" required />
            ${phoneHtml}
            <button type="submit" class="leo-button"></button>
          </form>
          <div class="leo-success"></div>
          <div class="leo-footer-text"></div>
        </div>
      `;

      this.options.mountTo.appendChild(this.overlay);

      // Cache DOM Elements
      this.modal = this.overlay.querySelector(".leo-modal");
      this.closeBtn = this.overlay.querySelector(".leo-modal-close");
      this.form = this.overlay.querySelector("form");
      this.inputName = this.overlay.querySelector("#leo-name-input");
      this.inputEmail = this.overlay.querySelector("#leo-email-input");
      this.inputPhone = this.overlay.querySelector("#leo-phone-input");
      this.button = this.overlay.querySelector(".leo-button");
      this.successBox = this.overlay.querySelector(".leo-success");
      this.titleEl = this.overlay.querySelector(".leo-modal-title");
      this.descEl = this.overlay.querySelector(".leo-modal-description");
      this.footerText = this.overlay.querySelector(".leo-footer-text");

      this._applyI18n();
      this._bindEvents();
    }

    _bindEvents() {
      // Close events
      this.closeBtn.addEventListener("click", () => this.close());
      this.overlay.addEventListener("click", (e) => {
        if (e.target === this.overlay) this.close();
      });

      // Submit event
      this.form.addEventListener("submit", (e) => {
        e.preventDefault();

        const name = this.inputName.value.trim();
        const email = this.inputEmail.value.trim();
        let phone = "";

        // Validation
        if (!name || name.length < 2) return this._showError("name");
        if (!this._validateEmail(email)) return this._showError("email");

        if (this.options.collectPhone && this.inputPhone) {
          phone = this.inputPhone.value.trim();
          // Phone Validation: Must be numeric digits only check
          if (!this._validatePhone(phone)) return this._showError("phone");
        }

        const payload = { name, email, phone };

        this._dispatch("leo.newsletter.submit", payload);

        // Show Success
        this.successBox.style.display = "block";
        this.form.style.display = "none";
        this._dispatch("leo.newsletter.success", payload);

        setTimeout(() => {
          this.form.reset();
          this.successBox.style.display = "none";
          this.form.style.display = "block";
          this.close();
        }, this.options.autoCloseDelay);
      });
    }

    _showError(field) {
      alert(`Invalid ${field}. Please check your input.`);
      // Focus field
      if (field === "name") this.inputName.focus();
      if (field === "email") this.inputEmail.focus();
      if (field === "phone") this.inputPhone.focus();
    }

    _validateEmail(email) {
      return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    _validatePhone(phone) {
      // 1. Strict Character Check:
      // Allow ONLY Digits (0-9), Dash (-), Space ( ), and Plus (+)
      // If the string contains letters (a-z) or symbols (@, #, etc), this returns false.
      const validChars = /^[0-9\-\+ ]+$/;
      if (!validChars.test(phone)) {
        return false;
      }

      // 2. Length Check:
      // Strip dashes/spaces to count the actual digits
      const digits = phone.replace(/\D/g, "");
      
      // Standard international phones are between 7 and 15 digits
      return digits.length >= 7 && digits.length <= 15;
    }

    _applyI18n() {
      const dict = this.options.i18n[this.options.locale] || {};
      this.titleEl.textContent = dict.title || "LEO CDP Updates";
      this.descEl.textContent =
        dict.description || "Get product updates directly to your inbox.";
      this.inputName.placeholder = dict.placeholderName || "Your name";
      this.inputEmail.placeholder = dict.placeholderEmail || "Your work email";
      if (this.inputPhone)
        this.inputPhone.placeholder = dict.placeholderPhone || "Phone number";
      this.button.textContent = dict.button || "Subscribe";
      this.footerText.textContent =
        dict.footer || "No spam. Unsubscribe anytime.";
      this.successBox.textContent =
        dict.success || "Thank you! You have successfully subscribed.";
    }

    _bindTriggers() {
      document.querySelectorAll(this.options.triggerSelector).forEach((el) => {
        el.addEventListener("click", (e) => {
          e.preventDefault();
          this.open(el.getAttribute("data-leo-newsletter"));
        });
      });
    }

    _dispatch(name, detail) {
      document.dispatchEvent(new CustomEvent(name, { detail }));
    }

    // ======================
    // Private: CSS Injector
    // ======================
    _injectStyles() {
      const styleId = "leo-newsletter-styles";
      if (document.getElementById(styleId)) return; // Prevent duplicate injection

      const css = `
        /* Variables */
        :root {
          --leo-z-index: 9999;
          --leo-font: 'Inter', system-ui, -apple-system, sans-serif;
        }

        /* Light Theme (Default) */
        .leo-theme-light {
          --leo-bg: #ffffff;
          --leo-text-primary: #111827;
          --leo-text-secondary: #6b7280;
          --leo-input-bg: #ffffff;
          --leo-input-border: #d1d5db;
          --leo-input-text: #111827;
          --leo-btn-close-bg: #f3f4f6;
          --leo-btn-close-text: #6b7280;
        }

        /* Dark Theme */
        .leo-theme-dark {
          --leo-bg: #1f2937;
          --leo-text-primary: #f9fafb;
          --leo-text-secondary: #9ca3af;
          --leo-input-bg: #374151;
          --leo-input-border: #4b5563;
          --leo-input-text: #f3f4f6;
          --leo-btn-close-bg: #374151;
          --leo-btn-close-text: #9ca3af;
        }

        .leo-modal-overlay {
          position: fixed;
          inset: 0;
          background: rgba(0, 0, 0, 0.5);
          display: none; /* Toggled by JS */
          align-items: center;
          justify-content: center;
          z-index: var(--leo-z-index);
          opacity: 0;
          transition: opacity 0.25s ease;
        }

        .leo-modal-overlay.active {
          opacity: 1;
        }

        .leo-modal {
          width: 420px;
          max-width: 90%;
          background: var(--leo-bg);
          color: var(--leo-text-primary);
          border-radius: 12px;
          box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
          padding: 32px 24px;
          font-family: var(--leo-font);
          position: relative;
          transform: translateY(10px);
          transition: transform 0.25s ease;
        }

        .leo-modal-overlay.active .leo-modal {
          transform: translateY(0);
        }

        .leo-modal-close {
          position: absolute;
          right: 16px;
          top: 16px;
          width: 32px;
          height: 32px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 20px;
          cursor: pointer;
          border: none;
          background-color: var(--leo-btn-close-bg);
          color: var(--leo-btn-close-text);
          border-radius: 50%;
          transition: filter 0.2s;
        }
        .leo-modal-close:hover { filter: brightness(0.9); }

        .leo-modal-title {
          font-size: 22px;
          font-weight: 700;
          margin-bottom: 8px;
          color: var(--leo-text-primary);
          line-height: 1.2;
        }

        .leo-modal-description {
          font-size: 15px;
          color: var(--leo-text-secondary);
          margin-bottom: 24px;
          line-height: 1.5;
        }

        .leo-input {
          display: block;
          width: 100%;
          padding: 12px 16px;
          margin-bottom: 12px;
          border-radius: 8px;
          border: 1px solid var(--leo-input-border);
          background: var(--leo-input-bg);
          color: var(--leo-input-text);
          font-size: 15px;
          outline: none;
          transition: border-color 0.2s ease;
          box-sizing: border-box;
        }

        .leo-input:focus {
          border-color: #2563eb;
          box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
        }

        .leo-button {
          width: 100%;
          margin-top: 8px;
          padding: 14px;
          border-radius: 8px;
          border: none;
          background: #2563eb;
          color: #fff;
          font-size: 16px;
          font-weight: 600;
          cursor: pointer;
          transition: background 0.2s ease;
        }

        .leo-button:hover { background: #1d4ed8; }

        .leo-footer-text {
          margin-top: 16px;
          font-size: 13px;
          color: var(--leo-text-secondary);
          text-align: center;
        }

        .leo-success {
          margin-top: 16px;
          padding: 12px;
          border-radius: 8px;
          background: #d1fae5;
          color: #065f46;
          font-weight: 500;
          font-size: 14px;
          text-align: center;
          display: none;
        }
        
        /* Dark mode specific override for success message */
        .leo-theme-dark .leo-success {
          background: #064e3b;
          color: #ecfdf5;
        }
      `;

      const style = document.createElement("style");
      style.id = styleId;
      style.textContent = css;
      document.head.appendChild(style);
    }
  }

  window.LeoNewsletterModal = LeoNewsletterModal;
})(window);
