/**
 * DataHubAuth Library (JWT SSO Edition)
 * Embeddable auth and API handler.
 */
const DataHubAuth = (function() {
    const config = {
        STORAGE_KEY: 'axis_user',
        TIME_TO_LIVE: 120,
        loginUrl: '',
        apiUrl: '',
        containerId: ''
    };

    const injectStyles = () => {
        if ($('#dha-styles').length) return;
        $('head').append(`
            <style id="dha-styles">
                #dha-overlay { position: fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.5); display:flex; justify-content:center; align-items:center; z-index:99999; }
                .dha-box { background:#fff; padding:30px; border-radius:12px; width:100%; max-width:350px; box-shadow:0 15px 35px rgba(0,0,0,0.2); font-family:-apple-system, sans-serif; }
                .dha-input { width:100%; padding:10px; margin:10px 0; border:1px solid #ccc; border-radius:4px; box-sizing:border-box; }
                .dha-btn { width:100%; padding:10px; background:#2563eb; color:white; border:none; border-radius:4px; cursor:pointer; font-weight:600; }
                .dha-btn:disabled { background:#94a3b8; }
            </style>
        `);
    };

    const showLogin = () => {
        injectStyles();
        if ($('#dha-overlay').length) return;
        $('body').append(`
            <div id="dha-overlay">
                <div class="dha-box">
                    <h2 style="margin-top:0">SSO Login</h2>
                    <form id="dha-form">
                        <input type="email" id="dha-email" class="dha-input" placeholder="Email" required>
                        <input type="password" id="dha-password" class="dha-input" placeholder="Password" required>
                        <button type="submit" class="dha-btn" id="dha-submit">Sign In</button>
                    </form>
                </div>
            </div>
        `);
        $('#dha-form').on('submit', handleLogin);
    };

    const handleLogin = function(e) {
        e.preventDefault();
        const $btn = $('#dha-submit').prop('disabled', true).text('Authenticating...');
        
        $.ajax({
            url: config.loginUrl,
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: $('#dha-email').val(), password: $('#dha-password').val() }),
            success: (res) => {
              
                // Expecting JWT in res.token
                lscache.set(config.STORAGE_KEY, { token: res.token }, config.TIME_TO_LIVE ); // 2 hours
                $('#dha-overlay').remove();
                fetchData();
            },
            error: () => {
                alert('Invalid credentials or SSO unreachable.');
                $btn.prop('disabled', false).text('Sign In');
            }
        });
    };

    const fetchData = function() {
        const session = lscache.get(config.STORAGE_KEY);
        if (!session || !session.token) return showLogin();

        $.ajax({
            url: config.apiUrl,
            headers: { 'Authorization': 'Bearer ' + session.token },
            success: (data) => {
                $('#' + config.containerId).html(`<div>${JSON.stringify(data)}</div>`);
            },
            error: (xhr) => {
                if (xhr.status === 401) { // JWT expired or invalid
                    lscache.remove(config.STORAGE_KEY);
                    showLogin();
                }
            }
        });
    };

    return {
        init: (opts) => {
            config.loginUrl = opts.loginUrl;
            config.apiUrl = opts.apiUrl;
            config.containerId = opts.containerId;
            $(document).ready(fetchData);
        }
    };
})();