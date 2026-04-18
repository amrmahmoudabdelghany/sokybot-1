package org.sokybot.login.workflow.builtin;

/**
 * Persistent keys, state ids, and shared constants for the login workflow cycle.
 */
final class LoginCycleKeys {

    static final String CYCLE_NAME = "login-cycle";

    static final String STATE_CHECK_CONNECTION = "CHECK_CONNECTION";
    static final String STATE_POST_CHECK_CONNECTION = "POST_CHECK_CONNECTION";
    static final String STATE_CONNECT_TO_GATEWAY = "CONNECT_TO_GATEWAY";
    static final String STATE_WAIT_FOR_CONNECTION = "WAIT_FOR_CONNECTION";
    static final String STATE_RESEND_AGENT_LIST_REQUEST = "RESEND_AGENT_LIST_REQUEST";
    static final String STATE_WAIT_FOR_AGENTS = "WAIT_FOR_AGENTS";
    static final String STATE_WAIT_FOR_AGENTS_RECHECK = "WAIT_FOR_AGENTS_RECHECK";
    static final String STATE_CHECK_AGENT_WAIT_TIMEOUT = "CHECK_AGENT_WAIT_TIMEOUT";
    static final String STATE_CONNECTED_RESUME = "CONNECTED_RESUME";
    static final String STATE_PAUSE_BEFORE_GATEWAY_LOGIN = "PAUSE_BEFORE_GATEWAY_LOGIN";
    static final String STATE_PARK_MISSING_LOGIN = "PARK_MISSING_LOGIN";
    static final String STATE_SEND_LOGIN_REQUEST = "SEND_LOGIN_REQUEST";
    static final String STATE_WAIT_FOR_LOGIN_RESPONSE = "WAIT_FOR_LOGIN_RESPONSE";
    static final String STATE_WAIT_FOR_LOGIN_RECHECK = "WAIT_FOR_LOGIN_RECHECK";
    static final String STATE_SUBMIT_GATEWAY_IMAGE_CODE = "SUBMIT_GATEWAY_IMAGE_CODE";
    static final String STATE_CHECK_LOGIN_FAILED_IMMEDIATE = "CHECK_LOGIN_FAILED_IMMEDIATE";
    static final String STATE_CHECK_LOGIN_TIMEOUT = "CHECK_LOGIN_TIMEOUT";
    static final String STATE_CHECK_AGENT_SERVER_CONNECTION_FAILURE = "CHECK_AGENT_SERVER_CONNECTION_FAILURE";
    static final String STATE_WAIT_FOR_AGENT_SERVER_CONNECTION_RECHECK = "WAIT_FOR_AGENT_SERVER_CONNECTION_RECHECK";
    static final String STATE_WAIT_FOR_AGENT_AUTH = "WAIT_FOR_AGENT_AUTH";
    static final String STATE_WAIT_FOR_AGENT_AUTH_RECHECK = "WAIT_FOR_AGENT_AUTH_RECHECK";
    static final String STATE_CHECK_AGENT_AUTH_TIMEOUT = "CHECK_AGENT_AUTH_TIMEOUT";
    static final String STATE_WAIT_FOR_CHARACTER = "WAIT_FOR_CHARACTER";
    static final String STATE_WAIT_FOR_CHARACTER_RECHECK = "WAIT_FOR_CHARACTER_RECHECK";
    static final String STATE_CHECK_CHARACTER_WAIT_TIMEOUT = "CHECK_CHARACTER_WAIT_TIMEOUT";
    static final String STATE_RETRY_DELAY = "RETRY_DELAY";
    static final String STATE_WAIT_RETRY_DELAY = "WAIT_RETRY_DELAY";
    static final String STATE_WAIT_RETRY_DELAY_RECHECK = "WAIT_RETRY_DELAY_RECHECK";
    static final String STATE_WAIT_FOR_AGENT_SERVER_CONNECTION = "WAIT_FOR_AGENT_SERVER_CONNECTION";
    static final String STATE_CHECK_CONNECTION_ROUTE = "CHECK_CONNECTION_ROUTE";
    static final String STATE_PARK_MISSING_PREREQS = "PARK_MISSING_PREREQS";
    static final String STATE_PARK_MANUAL_CONNECT = "PARK_MANUAL_CONNECT";

    static final String KEY_LOGIN_HALTED_CREDENTIAL = "loginHaltedCredentialFailure";
    static final String KEY_LOGIN_LAST_HALT_SIGNATURE = "loginLastHaltSignature";
    static final String KEY_RUNTIME_LOGIN_SETTINGS = "runtimeLoginSettings";
    static final String KEY_UI_LOGIN_PHASE_EVENT_DEDUP = "uiLoginPhaseEventDedup";
    static final String KEY_AGENT_WAIT_DEADLINE_MS = "loginAgentWaitDeadlineMs";
    static final String KEY_AGENT_WAIT_TIMED_OUT = "loginAgentWaitTimedOut";
    static final String KEY_LOGIN_RESPONSE_DEADLINE_MS = "loginResponseDeadlineMs";
    static final String KEY_AGENT_AUTH_DEADLINE_MS = "agentAuthDeadlineMs";
    static final String KEY_RETRY_UNTIL_MS = "loginRetryUntilMs";
    static final String KEY_ATTEMPT_LOGIN_SETTINGS = "loginAttemptSettings";
    static final String KEY_ACTIVE_ATTEMPT_ID = "loginActiveAttemptId";
    static final String KEY_RETRY_ATTEMPT_ID = "loginRetryAttemptId";
    static final String KEY_INTERACTIVE_WAIT_UNTIL_MS = "loginInteractiveWaitUntilMs";
    static final String KEY_USER_RESUME_REQUIRED = "loginUserResumeRequired";
    static final String KEY_LOGIN_IN_PROGRESS = "loginInProgress";
    static final String KEY_AGENT_BYPASS_COOLDOWN_UNTIL_MS = "agentBypassCooldownUntilMs";
    static final String KEY_AGENT_LIST_CACHE_AT_MS = "agentListCacheAtMs";
    static final String KEY_LAST_GATEWAY_LOGIN_REQUEST_MS = "loginLastGatewayLoginRequestMs";
    static final String KEY_NETWORK_TRANSITIONS = "networkTransitions";
    static final String KEY_GATEWAY_ENDPOINTS = "loginGatewayEndpoints";
    static final String KEY_GATEWAY_ENDPOINT_INDEX = "loginGatewayEndpointIndex";
    static final String KEY_GATEWAY_KNOWN_GOOD_INDEX = "loginGatewayKnownGoodIndex";
    static final String KEY_GATEWAY_CONNECTED_AT_MS = "loginGatewayConnectedAtMs";
    static final String KEY_GATEWAY_DNS_RESOLVED_AT_MS = "loginGatewayDnsResolvedAtMs";
    static final String KEY_VOLATILE_STATE = "loginVolatileState";
    static final String KEY_VOLATILE_RESET_TOKEN = "resetToken";
    static final String KEY_ERROR_THROTTLE = "loginErrorThrottle";

    static final String PHASE_MISSING_GATEWAY = "MISSING_GATEWAY";
    static final String PHASE_PENDING_MANUAL_CONNECT = "PENDING_MANUAL_CONNECT";
    static final String PHASE_MISSING_CREDENTIALS = "MISSING_CREDENTIALS";
    static final String PHASE_MISSING_AGENT_SERVER = "MISSING_AGENT_SERVER";
    static final String PHASE_MISSING_CHARACTER_SELECTION = "MISSING_CHARACTER_SELECTION";
    static final String PHASE_WAITING_FOR_AGENTS = "WAITING_FOR_AGENTS";
    static final String PHASE_GATEWAY_LOGIN_PAUSE = "GatewayLoginPause";
    static final String PHASE_WAITING_FOR_AGENTS_TIMEOUT = "WAITING_FOR_AGENTS_TIMEOUT";
    static final String PHASE_WAITING_FOR_PASSCODE = "WAITING_FOR_PASSCODE";
    static final String PHASE_WAIT_FOR_CAPTCHA = "WAIT_FOR_CAPTCHA";
    static final String PHASE_IN_QUEUE = "IN_QUEUE";
    static final String PHASE_LOADING_ENVIRONMENT = "LOADING_ENVIRONMENT";
    static final String PHASE_SERVER_INSPECTION = "SERVER_INSPECTION";

    static final String FAILURE_NETWORK = "NETWORK";
    static final String FAILURE_AGENT_TIMEOUT = "AGENT_TIMEOUT";
    static final String FAILURE_CREDENTIAL = "CREDENTIAL";
    static final String FAILURE_GHOST_COOLDOWN = "GHOST_COOLDOWN";
    static final String FAILURE_MANUAL_VERIFICATION = "MANUAL_VERIFICATION";
    static final String FAILURE_CHARACTER_NOT_FOUND = "CHARACTER_NOT_FOUND";
    static final String FAILURE_SERVER_INSPECTION = "SERVER_INSPECTION";
    static final String FAILURE_MISSING_PREREQ = "MISSING_PREREQ";
    static final String FAILURE_UNKNOWN_RETRY = "UNKNOWN_RETRY";
    static final String FAILURE_FATAL = "FATAL";

    static final long AGENT_WAIT_TIMEOUT_MS = 15000L;
    static final long LOGIN_RESPONSE_TIMEOUT_MS = 15000L;
    static final long AGENT_AUTH_TIMEOUT_MS = 45000L;
    static final long PASSCODE_USER_INPUT_TIMEOUT_MS = 60000L;
    static final long LOGOUT_ACK_TIMEOUT_MS = 3000L;
    static final long DNS_STICKY_TTL_MS = 60000L;
    static final long AGENT_REQUEST_MIN_INTERVAL_MS = 10_000L;

    static final long MIN_RETRY_DELAY_MS = 5000L;
    static final long MAX_RETRY_DELAY_MS = 300000L;

    private LoginCycleKeys() {
    }
}
