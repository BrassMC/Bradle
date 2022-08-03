import net.minecraft.client.main.Main;

import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        Main.main(new String[] {
                "--accessToken", "1",
                "--version", "1.19.1"
        });
    }
}
