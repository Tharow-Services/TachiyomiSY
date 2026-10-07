package eu.kanade.tachiyomi.source.online.all.webtoon;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HMACSubstitution {

  public static final char[] SUBSTITUTION_TABLE = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
    'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
    'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4',
    '5', '6', '7', '8', '9', '+', '/'};

  public static String substitution(byte[] bArr) {
    int length = bArr.length;
    int i12 = length / 3;
    int i13 = length - (i12 * 3);
    StringBuilder stringBuffer = new StringBuilder(((length + 2) / 3) * 4);
    char[] cArr = SUBSTITUTION_TABLE;
    int i14 = 0;
    for (int i15 = 0; i15 < i12; i15++) {
      int i16 = bArr[i14] & 255;
      int i17 = i14 + 2;
      int i18 = bArr[i14 + 1] & 255;
      i14 += 3;
      byte b12 = bArr[i17];
      stringBuffer.append(cArr[i16 >> 2]);
      stringBuffer.append(cArr[((i16 << 4) & 63) | (i18 >> 4)]);
      stringBuffer.append(cArr[((i18 << 2) & 63) | ((b12 & 255) >> 6)]);
      stringBuffer.append(cArr[b12 & 63]);
    }
    if (i13 != 0) {
      int i19 = i14 + 1;
      int i22 = bArr[i14] & 255;
      stringBuffer.append(cArr[i22 >> 2]);
      if (i13 == 1) {
        stringBuffer.append(cArr[(i22 << 4) & 63]);
        stringBuffer.append("==");
      } else {
        int i23 = bArr[i19] & 255;
        stringBuffer.append(cArr[((i22 << 4) & 63) | (i23 >> 4)]);
        stringBuffer.append(cArr[(i23 << 2) & 63]);
        stringBuffer.append('=');
      }
    }
    return stringBuffer.toString();
  }

  /**
  / wtu android_id
  */
  public static String getWTU(String value) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("MD5");
      messageDigest.update(value.getBytes());
      byte[] bArrDigest = messageDigest.digest();
      StringBuilder stringBuffer = new StringBuilder();
      for (byte b12 : bArrDigest) {
        stringBuffer.append(Integer.toString((b12 & 255) + 256, 16).substring(1));
      }
      return stringBuffer.toString();
    } catch (NoSuchAlgorithmException e12) {
      return "";
    }
  }
}
