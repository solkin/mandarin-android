package com.tomclaw.mandarin.im.icq;

import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/19/13
 * Time: 10:43 AM
 */
public class WimConstants {

    public static final String TOKEN_A = "a";
    public static final String CLIENT_LOGIN_URL = "https://api.login.icq.net/auth/clientLogin";
    public static final String NORMALIZE_PHONE_URL = "https://www.icq.com/smsreg/normalizePhoneNumber.php";
    public static final String VALIDATE_PHONE_URL = "https://www.icq.com/smsreg/requestPhoneValidation.php";
    public static final String LOGIN_PHONE_URL = "https://www.icq.com/smsreg/loginWithPhoneNumber.php";
    public static final String CLIENT_NAME = "clientName";
    public static final String CLIENT_VERSION = "clientVersion";
    public static final String DEV_ID = "devId";
    public static final String FORMAT = "f";
    public static final String ID_TYPE = "idType";
    public static final String PASSWORD = "pwd";
    public static final String LOGIN = "s";
    public static final String RESPONSE_OBJECT = "response";
    public static final String DATA_OBJECT = "data";
    public static final String USER_DATA_OBJECT = "userData";
    public static final String LOGIN_ID = "loginId";
    public static final String HOST_TIME = "hostTime";
    public static final String STATUS_CODE = "statusCode";
    public static final String SESSION_SECRET = "sessionSecret";
    public static final String SESSION_KEY = "sessionKey";
    public static final String RENEW_TOKEN = "renewToken";
    public static final String TOKEN_OBJECT = "token";
    public static final String EXPIRES_IN = "expiresIn";
    public static final String ASSERT_CAPS = "assertCaps";
    public static final String BUILD_NUMBER = "buildNumber";
    public static final String DEVICE_ID = "deviceId";
    public static final String EVENTS = "events";
    public static final String IMF = "imf";
    public static final String INCLUDE_PRESENCE_FIELDS = "includePresenceFields";
    public static final String INVISIBLE = "invisible";
    public static final String DEV_ID_K = "k";
    public static final String LANGUAGE = "language";
    public static final String MINIMIZE_RESPONSE = "minimizeResponse";
    public static final String MOBILE = "mobile";
    public static final String POLL_TIMEOUT = "pollTimeout";
    public static final String RAW_MSG = "rawMsg";
    public static final String SESSION_TIMEOUT = "sessionTimeout";
    public static final String TS = "ts";
    public static final String VIEW = "view";
    public static final String START_SESSION_URL = "https://api.icq.net/aim/startSession";
    public static final String RENEW_TOKEN_URL = "https://api.login.icq.net/auth/getInfo";
    public static final String POST_PREFIX = "POST&";
    public static final String AMP = "&";
    public static final String EQUAL = "=";
    public static final String AIM_SID = "aimsid";
    public static final String FETCH_BASE_URL = "fetchBaseURL";
    public static final String TYPING = "typing";
    public static final String TYPING_STATUS = "typingStatus";
    public static final String TYPING_STATUS_NONE = "none";
    public static final String TYPING_STATUS_TYPE = "typing";
    public static final String MY_INFO = "myInfo";
    public static final String SESSION_ENDED = "sessionEnded";
    public static final String WELL_KNOWN_URLS = "wellKnownUrls";
    public static final String EVENTS_ARRAY = "events";
    public static final String TYPE = "type";
    public static final String EVENT_DATA_OBJECT = "eventData";
    public static final String TIMEOUT = "timeout";
    public static final String R_PARAM = "r";
    public static final String PEEK = "peek";
    public static final String BUDDYLIST = "buddylist";
    public static final String GROUPS_ARRAY = "groups";
    public static final String NAME = "name";
    public static final String ID_FIELD = "id";
    public static final String BUDDIES_ARRAY = "buddies";
    public static final String AIM_ID = "aimId";
    public static final String FRIENDLY = "friendly";
    public static final String DISPLAY_ID = "displayId";
    public static final String STATE = "state";
    public static final String USER_TYPE = "userType";
    public static final String BUDDY_ICON = "buddyIcon";
    public static final String BIG_BUDDY_ICON = "bigBuddyIcon";
    public static final String LAST_SEEN = "lastseen";
    public static final String HIST_DLG_STATE = "histDlgState";
    public static final String IM = "im";
    public static final String MESSAGE = "message";
    public static final String MSG_ID = "msgId";
    public static final String AUTORESPONSE = "autoresponse";
    public static final String SOURCE_OBJECT = "source";
    public static final String PRESENCE = "presence";
    public static final String STATUS_MSG = "statusMsg";
    public static final String MOOD_TITLE = "moodTitle";
    public static final String MOOD_ICON = "moodIcon";
    public static final String TIMESTAMP = "timestamp";
    public static final String QUE = "?";
    public static final String OFFLINE_IM = "offlineIM";
    public static final String IM_STATE = "imState";
    public static final String SEND_REQ_ID = "sendReqId";
    public static final String IM_STATES_ARRAY = "imStates";
    public static final String REQUEST_ID = "requestId";
    public static final String FORMAT_JSON = "json";
    public static final String COUNTRY_CODE = "countryCode";
    public static final String PHONE_NUMBER = "phoneNumber";
    public static final String MSISDN = "msisdn";
    public static final String LOCALE = "locale";
    public static final String SMS_FORMAT_TYPE = "smsFormatType";
    public static final String HUMAN = "human";
    public static final String CLIENT = "client";
    public static final String ICQ = "icq";
    public static final String TRANS_ID = "trans_id";
    public static final String SMS_CODE = "sms_code";
    public static final String CREATE_ACCOUNT = "create_account";
    public static final String SIG_SHA256 = "sig_sha256";
    public static final String[] IM_STATES = new String[]{
            "unknown",
            "failed",
            "sending",
            "sent",
            "delivered"
    };
    private static final String PROTOCOL_REGEX =
            "http" + "|" + "https";
    private static final String DOMAINS_REGEX =
            "files\\.mail\\.ru" + "|" +
                    "api\\.icq\\.net" + "|" +
                    "files\\.icq\\.net" + "|" +
                    "files\\.icq\\.com" + "|" +
                    "(?:files\\.)?chat\\.my\\.com";
    public static final Pattern URL_REGEX = Pattern.compile(
            "(?:" + PROTOCOL_REGEX + ")://(?:" + DOMAINS_REGEX + ")/(?:get/|files/(?:get\\?fileId=)?)?([0-9a-zA-Z_\\-]+)");
}
