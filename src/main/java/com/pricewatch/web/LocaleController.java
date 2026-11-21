package com.pricewatch.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;
import java.util.Set;

/**
 * J12 language and contrast preferences. Plain first-party cookies keep the
 * choice across restarts; no account or server-side session is involved.
 */
@Controller
public class LocaleController {
    static final String LANG_COOKIE = "LANG";
    static final String CONTRAST_COOKIE = "CONTRAST";
    private static final Set<String> LANGUAGES = Set.of("en", "zh_TW", "zh_CN");

    @GetMapping("/lang")
    String language(@RequestParam(defaultValue = "en") String code,
                    @RequestParam(defaultValue = "/") String back,
                    HttpServletResponse response) {
        String chosen = LANGUAGES.contains(code) ? code : "en";
        Cookie cookie = new Cookie(LANG_COOKIE, chosen);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 365);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        return "redirect:" + safeBack(back);
    }

    @GetMapping("/contrast")
    String contrast(@RequestParam(defaultValue = "off") String mode,
                    @RequestParam(defaultValue = "/") String back,
                    HttpServletResponse response) {
        Cookie cookie = new Cookie(CONTRAST_COOKIE, "high".equals(mode) ? "high" : "off");
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 365);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        return "redirect:" + safeBack(back);
    }

    /** Only redirect within this app; a crafted value cannot leave the host. */
    /** Locale for this request: set by the resolver; falls back to the LANG cookie. */
    static Locale requestLocale(HttpServletRequest request) {
        Locale resolved = org.springframework.context.i18n.LocaleContextHolder.getLocale();
        if (resolved != null && !"en".equals(resolved.toLanguageTag())) return resolved;
        return currentLocale(request);
    }

    /** Only redirect within this app; a crafted value cannot leave the host. */
    static String safeBack(String back) {
        if (back == null || !back.startsWith("/") || back.startsWith("//")) return "/";
        return back;
    }

    /** Active UI language from the LANG cookie, defaulting to English. */
    static Locale currentLocale(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (LANG_COOKIE.equals(cookie.getName())) {
                    return fromCode(cookie.getValue());
                }
            }
        }
        return Locale.ENGLISH;
    }

    static Locale fromCode(String code) {
        return switch (code == null ? "" : code) {
            case "zh_TW", "zh-TW", "zh-HK" -> Locale.forLanguageTag("zh-TW");
            case "zh_CN", "zh-CN", "zh-SG" -> Locale.forLanguageTag("zh-CN");
            default -> Locale.ENGLISH;
        };
    }

    static String htmlLang(Locale locale) {
        String tag = locale == null ? "en" : locale.toLanguageTag();
        return switch (tag) {
            case "zh-TW", "zh-HK" -> "zh-Hant";
            case "zh-CN", "zh-SG" -> "zh-Hans";
            default -> "en";
        };
    }

    static String languageCode(Locale locale) {
        return switch (htmlLang(locale)) {
            case "zh-Hant" -> "zh_TW";
            case "zh-Hans" -> "zh_CN";
            default -> "en";
        };
    }

    static boolean highContrast(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        for (Cookie cookie : cookies) {
            if (CONTRAST_COOKIE.equals(cookie.getName())) return "high".equals(cookie.getValue());
        }
        return false;
    }

    /** Product label in the active UI language, falling back to English. */
    static String label(com.pricewatch.catalog.LocalizedText text, Locale locale) {
        if (text == null) return "";
        String lang = htmlLang(locale);
        if ("zh-Hant".equals(lang) && !blank(text.zhHant())) return text.zhHant();
        if ("zh-Hans".equals(lang) && !blank(text.zhHans())) return text.zhHans();
        return text.en();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
