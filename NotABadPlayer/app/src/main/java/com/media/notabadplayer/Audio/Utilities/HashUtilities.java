package com.media.notabadplayer.Audio.Utilities;

public class HashUtilities {
    public static int hashObjects(Object... values)
    {
        return hashCode(values);
    }

    public static int hashCode(Object a[])
    {
        if (a == null)
        {
            return 0;
        }

        int result = 1;

        for (Object element : a)
        {
            result = 31 * result + (element == null ? 0 : element.hashCode());
        }

        return result;
    }
}
