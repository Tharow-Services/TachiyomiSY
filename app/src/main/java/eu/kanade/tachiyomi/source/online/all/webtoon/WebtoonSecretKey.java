package eu.kanade.tachiyomi.source.online.all.webtoon;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/* JADX INFO: compiled from: HmacUtil.java */
/* JADX INFO: loaded from: classes11.dex */
public class WebtoonSecretKey {
  public static Mac getMACForKey(String str) throws NoSuchAlgorithmException, InvalidKeyException {
    SecretKeySpec secretKeySpec = new SecretKeySpec(str.getBytes(), "HmacSHA1");
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(secretKeySpec);
    return mac;
  }

  public static String b(String str, String str2) {
    String strSubstring = str.substring(0, Math.min(255, str.length()));
    StringBuilder sb2 = new StringBuilder();
    sb2.setLength(0);
    sb2.append(strSubstring);
    sb2.append(str2);
    return sb2.toString();
  }

  public static String getMacSub(Mac mac, String str) {
    byte[] bArrDoFinal;
    bArrDoFinal = mac.doFinal(str.getBytes());
    return HMACSubstitution.substitution(bArrDoFinal);
  }

  public static String getMACEncoded(Mac mac, String str, long j12) throws Exception {
    return getMACEncoded(mac, str, String.valueOf(Calendar.getInstance().getTimeInMillis() + j12));
  }

  public static String getMACEncoded(Mac mac, String str, String str2) throws UnsupportedEncodingException {
    String strEncode = URLEncoder.encode(HMACSubstitution.substitution(mac.doFinal(b(str, str2).getBytes())), "utf-8");
    StringBuilder sb2 = new StringBuilder();
    sb2.setLength(0);
    sb2.append(str);
    if (str.contains("?")) {
      sb2.append("&");
    } else {
      sb2.append("?");
    }
    sb2.append("msgpad=");
    sb2.append(str2);
    sb2.append("&md=");
    sb2.append(strEncode);
    return sb2.toString();
  }

}
