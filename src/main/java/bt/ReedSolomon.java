/*
  Reed Solomon Encoding and Decoding for Burst

  Version: 1.0, license: Public Domain, coder: NxtChg (admin@nxtchg.com)
  Java Version: ChuckOne (ChuckOne@mail.de).
*/
package bt;

public final class ReedSolomon {

  private static final int[] initial_codeword = {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  private static final int[] gexp = {1, 2, 4, 8, 16, 5, 10, 20, 13, 26, 17, 7, 14, 28, 29, 31, 27, 19, 3, 6, 12, 24, 21, 15, 30, 25, 23, 11, 22, 9, 18, 1};
  private static final int[] glog = {0, 0, 1, 18, 2, 5, 19, 11, 3, 29, 6, 27, 20, 8, 12, 23, 4, 10, 30, 17, 7, 22, 28, 26, 21, 25, 9, 16, 13, 14, 24, 15};
  private static final int[] codeword_map = {3, 2, 1, 0, 7, 6, 5, 4, 13, 14, 15, 16, 12, 8, 9, 10, 11};
  private static final String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

  private static final int base_32_length = 13;
  private static final int base_10_length = 20;

  static public long decode(String cypher_string) throws DecodeException {

    int[] codeword = new int[ReedSolomon.initial_codeword.length];
    System.arraycopy(ReedSolomon.initial_codeword, 0, codeword, 0, ReedSolomon.initial_codeword.length);

    int codeword_length = 0;
    for (int i = 0; i < cypher_string.length(); i++) {
      int position_in_alphabet = ReedSolomon.alphabet.indexOf(cypher_string.charAt(i));

      if (position_in_alphabet <= -1 || position_in_alphabet > ReedSolomon.alphabet.length()) {
        continue;
      }

      if (codeword_length > 16) {
        throw new CodewordTooLongException();
      }

      int codework_index = ReedSolomon.codeword_map[codeword_length];
      codeword[codework_index] = position_in_alphabet;
      codeword_length += 1;
    }

    if (codeword_length != 17 || !ReedSolomon.is_codeword_valid(codeword)) {
      throw new CodewordInvalidException();
    }

    int length = ReedSolomon.base_32_length;
    int[] cypher_string_32 = new int[length];
    for (int i = 0; i < length; i++) {
      cypher_string_32[i] = codeword[length - i - 1];
    }

    StringBuilder plain_string_builder = new StringBuilder();
    do { // base 32 to base 10 conversion
      int new_length = 0;
      int digit_10 = 0;

      for (int i = 0; i < length; i++) {
        digit_10 = digit_10 * 32 + cypher_string_32[i];

        if (digit_10 >= 10) {
          cypher_string_32[new_length] = digit_10 / 10;
          digit_10 %= 10;
          new_length += 1;
        } else if (new_length > 0) {
          cypher_string_32[new_length] = 0;
          new_length += 1;
        }
      }
      length = new_length;
      plain_string_builder.append((char)(digit_10 + (int)'0'));
    } while (length > 0);

    return Long.parseUnsignedLong(plain_string_builder.reverse().toString());
  }

  private static int gmult(int a, int b) {
    if (a == 0 || b == 0) {
      return 0;
    }

    int idx = (ReedSolomon.glog[a] + ReedSolomon.glog[b]) % 31;

    return ReedSolomon.gexp[idx];
  }

  private static boolean is_codeword_valid(int[] codeword) {
    int sum = 0;

    for (int i = 1; i < 5; i++) {
      int t = 0;

      for (int j = 0; j < 31; j++) {
        if (j > 12 && j < 27) {
          continue;
        }

        int pos = j;
        if (j > 26) {
          pos -= 14;
        }

        t ^= ReedSolomon.gmult(codeword[pos], ReedSolomon.gexp[(i * j) % 31]);
      }

      sum |= t;
    }

    return sum == 0;
  }

  public static long rsDecode(String rs) {
		if (rs != null && rs.length()!=32) {
			rs = rs.toUpperCase();
			if (rs.startsWith("BURST-")) {
				rs = rs.substring(6);
				try {
					long id = decode(rs);
					return id;
				} catch (Exception e) {
					//			logger.debug("Reed-Solomon decoding failed for " + rsString + ": " + e.toString());
					//			throw new RuntimeException(e.toString(), e);
				}
			}
		}
		return 0L;
	}

  abstract static class DecodeException extends Exception {
  }

  static final class CodewordTooLongException extends DecodeException {
  }

  static final class CodewordInvalidException extends DecodeException {
  }

  private ReedSolomon() {} // never
}


