import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


/*

https://drive.google.com/drive/folders/0B1j88lrqI04bUzQ5MUlpanhDMlk?resourcekey=0-OqE5HBgH60o5IKFQgJje7w

*/

public class AsciiArt {
	static boolean useCustomChars = true;
  public static int[] unicodeRange=new int[]{0x20,0xFE};
  public static char[] chars;// = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',' '};

	//basic = !"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{|}~
  
	//advanced = ¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüý

	/*line drawing characters
	─━│┃┄┅┆┇┈┉┊┋┌┍┎┏┐┑┒┓└┕┖┗┘┙┚┛├┝┞┟┠┡┢┣┤┥┦┧┨┩┪┫┬┭┮┯┰┱┲┳┴┵┶┷┸┹┺┻┼┽┾┿╀╁╂╃╄╅╆╇╈╉╊╋╌╍╎╏═║╒╓╔╕╖╗╘╙╚╛╜╝╞╟╠╡╢╣╤╥╦╧╨╩╪╫╬█░▒▓
*/
	public static String custom = "─━│┃┄┅┆┇┈┉┊┋┌┍┎┏┐┑┒┓└┕┖┗┘┙┚┛├┝┞┟┠┡┢┣┤┥┦┧┨┩┪┫┬┭┮┯┰┱┲┳┴┵┶┷┸┹┺┻┼┽┾┿╀╁╂╃╄╅╆╇╈╉╊╋╌╍╎╏═║╒╓╔╕╖╗╘╙╚╛╜╝╞╟╠╡╢╣╤╥╦╧╨╩╪╫╬█░▒▓ !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

	public static BufferedImage[] charImages;
  public static int fontSize=16;
  public static int[] patchSize=new int[]{10,20};//experimentall derived from font size
  public static int rowOffset=15;//experimentally derived from font size

	
	public static void main(String[] args) throws IOException, InterruptedException {


		//make char list
		if (!useCustomChars)
		{
			chars=new char[unicodeRange[1]-unicodeRange[0]];
			for(int n=0;n<chars.length;n++){
				chars[n]=(char)(unicodeRange[0]+n);
			}
		}else
		{
			chars=new char[custom.length()];
			for(int n=0;n<custom.length();n++){
				chars[n]=custom.charAt(n);
			}
		}
		
		System.out.print("using: ");
		System.out.println(chars);


		//make an image for each char
		Font font=new Font(Font.MONOSPACED,Font.PLAIN,fontSize);
		charImages=new BufferedImage[chars.length];
		for(int n=0;n<chars.length;n++){
			charImages[n]=new BufferedImage(patchSize[0],patchSize[1],BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D g=charImages[n].createGraphics();
			g.setFont(font);
			g.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			//System.out.println(font.getMaxCharBounds(g.getFontRenderContext()));
			//this works for font size 16 and patchSize 10,20; needs to be tweaked for changes
			g.drawChars(new char[]{chars[n]},0,1,0,rowOffset);
		}
		
		BufferedImage inputImg= ImageIO.read(new File("AsciiArt/sign.png"));
		BufferedImage grayImg=new BufferedImage(inputImg.getWidth(),inputImg.getHeight(),BufferedImage.TYPE_BYTE_GRAY);
		for(int x=0;x< inputImg.getWidth();x++){
			for(int y=0;y< inputImg.getHeight();y++){
				grayImg.setRGB(x,y,inputImg.getRGB(x,y));
			}
		}
		popImage(grayImg);


		//find optimal char for each patch
		long start=System.currentTimeMillis();
		char[][] ascii=new char[grayImg.getWidth()/patchSize[0]][grayImg.getHeight()/patchSize[1]];
		Thread[] threads=new Thread[ascii.length];
		for(int px=0;px<ascii.length;px++){
			int finalPx = px;
			threads[px]=new Thread(()->{
				for(int py=0;py<ascii[0].length;py++){
					int[] err=new int[chars.length];
					for(int n=0;n<chars.length;n++){
						err[n]=0;
						for(int dx=0;dx<patchSize[0];dx++)
						{
							for(int dy=0;dy<patchSize[1];dy++)
							{
								int temp=Math.abs((grayImg.getRGB(finalPx *patchSize[0]+dx,py*patchSize[1]+dy)&0xff) - (charImages[n].getRGB(dx,dy)&0xff));
								err[n]+=temp*temp;
							}
						}
					}
					int minerr=err[0];
					int minIdx=0;
					for(int n=1;n<err.length;n++){
						if(err[n]<minerr){
							minerr=err[n];
							minIdx=n;
						}
					}
					ascii[finalPx][py]=chars[minIdx];
				}
			});
			threads[px].start();
		}
		for(int n=0;n<threads.length;n++){
			threads[n].join();
		}
		System.out.println("finished in "+(System.currentTimeMillis()-start)+"ms");

		//create image from chars in ascii[][]
		BufferedImage asciiImg=new BufferedImage(grayImg.getWidth(),grayImg.getHeight(),BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g=asciiImg.createGraphics();
		g.setFont(font);
		g.setRenderingHint(
		RenderingHints.KEY_TEXT_ANTIALIASING,
		RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		for(int px=0;px<ascii.length;px++) {
			for (int py = 0; py < ascii[0].length; py++) {
				g.drawChars(new char[]{ascii[px][py]},0,1,px*patchSize[0],py*patchSize[1]+rowOffset);
			}
		}
		popImage(asciiImg);

		//print chars in ascii[][]
		for(int y=0;y<ascii[0].length;y++){
			for(int x=0;x<ascii.length;x++){
				System.out.print(ascii[x][y]);
			}
			System.out.print("\n");
		}
}

	public static void popImage(BufferedImage img){
		JFrame frame=new JFrame();
		frame.setSize(img.getWidth()+32,img.getHeight()+32);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(new JPanel(){
			public void paintComponent(Graphics g){
				g.drawImage(img,0,0,this);
			}
		});
		frame.setVisible(true);
	}
}
