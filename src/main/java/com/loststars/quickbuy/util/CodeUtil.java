package com.loststars.quickbuy.util;

import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CodeUtil {

    private static int width = 90;

    private static int height = 20;

    private static int codeCount = 4;

    private static int xx = 15;

    private static int codeY = 16;

    private static int fontHeight = 18;

    private static char[] codeSequence = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
            'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    public static Map<String, Object> generateCodeAndPic() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics gd = image.getGraphics();
        Random random = new Random();
        gd.setColor(Color.WHITE);
        gd.fillRect(0, 0, width, height);
        Font font = new Font("Fixedsys", Font.BOLD, fontHeight);
        gd.setFont(font);
        gd.setColor(Color.BLACK);
        gd.drawRect(0, 0, width - 1, height - 1);
        gd.setColor(Color.BLACK);
        for (int i = 0; i < 30; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int xl = random.nextInt(12);
            int yl = random.nextInt(12);
            gd.drawLine(x, y, x + xl, y + yl);
        }
        StringBuffer randomCode = new StringBuffer();
        int red = 0, green = 0, blue = 0;
        for (int i = 0; i < codeCount; i++) {
            // 得到随机产生的验证码数字。
            String code = String.valueOf(codeSequence[random.nextInt(36)]);
            // 产生随机的颜色分量来构造颜色值，这样输出的每位数字的颜色值都将不同。
            red = random.nextInt(255);
            green = random.nextInt(255);
            blue = random.nextInt(255);

            // 用随机产生的颜色将验证码绘制到图像中。
            gd.setColor(new Color(red, green, blue));
            gd.drawString(code, (i + 1) * xx, codeY);

            // 将产生的四个随机数组合在一起。
            randomCode.append(code);
        }
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("code", randomCode.toString());
        map.put("codePic", image);
        return map;
    }

    public static void main(String[] args) throws IOException {
        Map<String, Object> map = generateCodeAndPic();
        System.out.println(map.get("code"));
        OutputStream out = new FileOutputStream("D:/"+System.currentTimeMillis()+".jpg");
        ImageIO.write((RenderedImage) map.get("codePic"), "jpeg", out);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write((BufferedImage) map.get("codePic"), "jpeg", byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        BASE64Encoder base64Encoder = new BASE64Encoder();
        String encodedStr = base64Encoder.encode(bytes);
        System.out.println(encodedStr);
    }
}
