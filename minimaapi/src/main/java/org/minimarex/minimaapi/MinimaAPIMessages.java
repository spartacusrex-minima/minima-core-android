package org.minimarex.minimaapi;

public class MinimaAPIMessages {

    /**
     * BASE MINIMA CLASS
     */
    public static final String MINIMA_BASE_CLASS = "org.minimarex.minimacore";

    /**
     * BASE MESSAGES
     */

    //Register
    public static final String MINIMA_API_REGISTER              = MINIMA_BASE_CLASS +".REGISTER";
    public static final String MINIMA_API_PACKAGE_CLASS         = MINIMA_BASE_CLASS +".PACKAGE_CLASS";
    public static final String MINIMA_API_APP_UID               = MINIMA_BASE_CLASS +".APP_UID";
    public static final String MINIMA_API_REGISTER_MINIMAID     = MINIMA_BASE_CLASS +".REGISTER_MINIMAID";

    //Send a command to run
    public static final String MINIMA_API_CMD                   = MINIMA_BASE_CLASS +".CMD";
    public static final String MINIMA_API_CMD_ACTION            = MINIMA_BASE_CLASS +".CMD_ACTION";

    //Caller can consume a content:// file response for results too big for an Intent extra
    public static final String MINIMA_API_CMD_FILERESP          = MINIMA_BASE_CLASS +".CMD_FILERESP";

    //Send the response back
    public static final String MINIMA_API_RESPONSE              = MINIMA_BASE_CLASS +".RESPONSE";
    public static final String MINIMA_API_RESPONSE_ID           = MINIMA_BASE_CLASS +".RESPONSE_ID";
    public static final String MINIMA_API_RESPONSE_RESULT       = MINIMA_BASE_CLASS +".RESPONSE_RESULT";

    //Large response - payload is at this content:// URI instead of RESPONSE_RESULT
    public static final String MINIMA_API_RESPONSE_URI          = MINIMA_BASE_CLASS +".RESPONSE_URI";
    public static final String MINIMA_API_RESPONSE_LEN          = MINIMA_BASE_CLASS +".RESPONSE_LEN";

    //Send a global Broadcast Message
    public static final String MINIMA_API_NOTIFY                = MINIMA_BASE_CLASS +".NOTIFY";
    public static final String MINIMA_API_NOTIFY_DATA           = MINIMA_BASE_CLASS +".NOTIFY_DATA";
}
