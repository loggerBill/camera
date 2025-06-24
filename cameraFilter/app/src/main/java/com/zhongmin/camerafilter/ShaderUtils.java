package com.zhongmin.camerafilter;
import android.content.Context;
import android.content.res.Resources;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

public class ShaderUtils {

    public static String readShaderFromRawResource(Context context, int resourceId) {
        StringBuilder shaderSource = new StringBuilder();
        InputStream inputStream = null;
        InputStreamReader inputReader = null;
        BufferedReader bufferedReader = null;

        try {
            inputStream = context.getResources().openRawResource(resourceId);
            inputReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                shaderSource.append(line).append("\n");
            }
        } catch (Resources.NotFoundException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) bufferedReader.close();
                if (inputReader != null) inputReader.close();
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return shaderSource.toString();
    }
}
