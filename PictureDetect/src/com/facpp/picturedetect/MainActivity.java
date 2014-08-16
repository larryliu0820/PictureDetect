package com.facpp.picturedetect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

/**
 * A simple demo, get a picture form your phone<br />
 * Use the facepp api to detect<br />
 * Find all face on the picture, and mark them out.
 * @author moon5ckq
 */
public class MainActivity extends Activity {

	final private static String TAG = "MainActivity";
	final private int TAKE_PICTURE = 2;
	final private int PICTURE_CHOOSE = 1;
	public enum State{
	GET,
	CREATE,
	VERIFY,
	DETECT
	}
	private State currentState = State.GET;
	private ImageView imageView;
	private Bitmap tempImg;
	private Button buttonGetImage;
	private Button buttonDetect;
	private Button buttonCreate;
	private Button buttonPhoto;
	private Button buttonVerify;
	private TextView textView;
	private JSONObject jsonResponse;
	
	private List<String> faceId = new ArrayList<String>();
	private String tempId = null;
	private String personName = null;
	
	private Uri imageUri;
	
	final private HttpRequests httpRequests = new HttpRequests(
			"7ce635b3cc93ae431de9c82174082905", 
			"n4-z3AY6ZbVbGoEFZverm00nVgI5I_Wt", false, false);
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        buttonGetImage = (Button)this.findViewById(R.id.button1);
        buttonDetect = (Button)this.findViewById(R.id.button2);
        buttonCreate = (Button)this.findViewById(R.id.button3);
        buttonPhoto =(Button)this.findViewById(R.id.button4);
        buttonVerify =(Button)this.findViewById(R.id.button5);
        
        buttonCreate.setVisibility(View.INVISIBLE);
        buttonDetect.setVisibility(View.INVISIBLE);
        buttonVerify.setVisibility(View.INVISIBLE);
		
        buttonGetImage.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				//get a picture form your phone
				System.out.println("state = "+currentState);
				if(currentState.compareTo(State.GET) != 0)
					return;
				if(personName != null)
					try {
						httpRequests.personDelete(new PostParameters().setPersonName(personName));
						MainActivity.this.runOnUiThread(new Runnable() {
							public void run() {
								textView.setText("delete successful.");
							}
						});
					} catch (FaceppParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		        photoPickerIntent.setType("image/*");
		        startActivityForResult(photoPickerIntent, PICTURE_CHOOSE);
			}
		});
        
        buttonPhoto.setOnClickListener(new OnClickListener() {
        	public void onClick(View arg0){
        		if(currentState.compareTo(State.GET) != 0)
					return;
        		
        		Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        		File photo = new File(Environment.getExternalStorageDirectory(),  "Pic.jpg");
        	    intent.putExtra(MediaStore.EXTRA_OUTPUT,
        	            Uri.fromFile(photo));
        	    imageUri = Uri.fromFile(photo);
				
				startActivityForResult(intent,TAKE_PICTURE);
        	}
        });
        
        buttonVerify.setOnClickListener(new OnClickListener() {
        	public void onClick(View arg0){
        		if(!currentState.equals( State.VERIFY))
        			return;
        		currentState = State.GET;
        		new Thread(new Runnable(){
					public void run(){
		        		try {
		        			String[] facesets = {"FamousInHistory","StoryStone"};
		        			double maxSim = 0;
		        			int maxNum = 0;
		        			String bestFaceId = null;
		        			for(String faceset:facesets){
			        			jsonResponse=httpRequests.facesetGetInfo(new PostParameters().setFacesetName(faceset));
			        			String facesetId = jsonResponse.getString("faceset_id");
								jsonResponse=httpRequests.recognitionSearch(
										new PostParameters().
										setFacesetId(facesetId).
										setKeyFaceId(tempId));
								System.out.println("verifying... ");
								
								int facesNumber= jsonResponse.getJSONArray("candidate").length();
								double maxSimilarity = 0;
								int maxNumber = 0;
								for(int i = 0; i < facesNumber; i++){
									double currSimilarity = Double.parseDouble(jsonResponse.
											getJSONArray("candidate").getJSONObject(i).getString("similarity"));
									if(maxSimilarity<currSimilarity){
										maxNumber = i;
										maxSimilarity = currSimilarity;
									}
								}
								if(maxSimilarity>maxSim) {
									maxSim = maxSimilarity;
									maxNum = maxNumber;
								}
								bestFaceId = jsonResponse.getJSONArray("candidate").getJSONObject(maxNum).getString("face_id");
								System.out.println("bestFaceId: "+bestFaceId);
		        			}
							jsonResponse=httpRequests.infoGetFace(new PostParameters().setFaceId(bestFaceId));
							System.out.println(jsonResponse);
							
							String onlineFileName = jsonResponse.
									getJSONArray("face_info").getJSONObject(0).
									getJSONArray("person").getJSONObject(0).
									getString("person_name");
							String[] elements = onlineFileName.split("_");
							final String name = elements[1];
							
							System.out.println("elements[2].split(\".\")[0] = "+elements[2].split("[.]")[0]);
							String fileName = "face_"+elements[0]+elements[2].split("[.]")[0];
							final Drawable famousFace = getResources().getDrawable(getResources()
					                  .getIdentifier(fileName, "drawable", getPackageName()));
							
							MainActivity.this.runOnUiThread(new Runnable() {
								public void run() {
									imageView.setImageDrawable(famousFace);
									textView.setText("您上辈子是："+name);
								}
							});
							//delete person
							
							
							tempId=null;
							personName=null;
						} catch (FaceppParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							MainActivity.this.runOnUiThread(new Runnable() {
								public void run() {
									textView.setText("Network err...");
								}
							});
						} catch (JSONException e) {
							e.printStackTrace();
							MainActivity.this.runOnUiThread(new Runnable() {
								public void run() {
									textView.setText("No such thing.");
								}
							});
						}
					}
        		}).start();
        	}
        });
        
        buttonCreate.setOnClickListener(new OnClickListener(){
        	
        	public void onClick(View arg0){
        		if(!currentState.equals(State.CREATE))
					return;
        		if(jsonResponse == null || tempId == null)
					return;
        		textView.setText("Create!");
        		
        		FaceppCreate faceppCreate = new FaceppCreate();
        		faceppCreate.create();
        		buttonCreate.setVisibility(View.INVISIBLE);	
				buttonPhoto.setVisibility(View.INVISIBLE);
				buttonVerify.setVisibility(View.VISIBLE);
				currentState = State.VERIFY;
        	}
        });
        
        textView = (TextView)this.findViewById(R.id.textView1);
        
       
        buttonDetect.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if(currentState.compareTo(State.DETECT) != 0)
					return;
				textView.setText("Waiting ...");
				
				FaceppDetect faceppDetect = new FaceppDetect();
				faceppDetect.setDetectCallback(new Callback());
				faceppDetect.detect(tempImg);
				buttonDetect.setVisibility(View.INVISIBLE);
				buttonGetImage.setVisibility(View.INVISIBLE);
				buttonCreate.setVisibility(View.VISIBLE);
				currentState = State.CREATE;
			}
		});
        
        imageView = (ImageView)this.findViewById(R.id.imageView1);
        imageView.setImageBitmap(tempImg);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    	
    	//the image picker callback
    	switch(requestCode){
    	case PICTURE_CHOOSE:
    	{
    		if (intent != null) {
    			//The Android api ~~~ 
    			//Log.d(TAG, "idButSelPic Photopicker: " + intent.getDataString());
    			Cursor cursor = getContentResolver().query(intent.getData(), null, null, null, null);
    			cursor.moveToFirst();
    			int idx = cursor.getColumnIndex(ImageColumns.DATA);
    			String fileSrc = cursor.getString(idx); 
    			//Log.d(TAG, "Picture:" + fileSrc);
    			
    			//just read size
    			Options options = new Options();
    			options.inJustDecodeBounds = true;
    			tempImg = BitmapFactory.decodeFile(fileSrc, options);

    			//scale size to read
    			options.inSampleSize = Math.max(1, (int)Math.ceil(Math.max((double)options.outWidth / 1024f, (double)options.outHeight / 1024f)));
    			options.inJustDecodeBounds = false;
    			tempImg = BitmapFactory.decodeFile(fileSrc, options);
    			textView.setText("Clik Detect. ==>");
    			
    			imageView.setImageBitmap(tempImg);
    			buttonPhoto.setVisibility(View.INVISIBLE);
    	    	buttonDetect.setVisibility(View.VISIBLE);
    	    	currentState = State.DETECT;
    		}
    		else {
    			Log.d(TAG, "idButSelPic Photopicker canceled");
    		}
    		break;
    	}
    	case TAKE_PICTURE:
    	{
    		if (resultCode == Activity.RESULT_OK) {
                Uri selectedImage = imageUri;
                getContentResolver().notifyChange(selectedImage, null);
                ContentResolver cr = getContentResolver();
                try {
                	tempImg = android.provider.MediaStore.Images.Media
                     .getBitmap(cr, selectedImage);

                    imageView.setImageBitmap(tempImg);
                    Toast.makeText(this, selectedImage.toString(),
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT)
                            .show();
                    Log.e("Camera", e.toString());
                }
                buttonPhoto.setVisibility(View.INVISIBLE);
            	buttonDetect.setVisibility(View.VISIBLE);
            	currentState = State.DETECT;
            }			
    		break;
    	}
    	}
    	
    }

    private class FaceppCreate {
    	
    	public void create(){
    		
    		new Thread(new Runnable(){
    			
    			public void run() {
    				try {
    					
    					int numOfFaces = jsonResponse.getJSONArray("face").length();
    					if(numOfFaces>1){
    						MainActivity.this.runOnUiThread(new Runnable(){
                				public void run() {
                					textView.setText("too many faces.");
                				}
                			});
    					}
    					//create a person
    					
    					personName = "person_1";
    					PostParameters params = new PostParameters().setPersonName(personName).setFaceId(tempId);
    					try{
    						jsonResponse=httpRequests.personCreate(params);
    					}catch(FaceppParseException e) {
    						e.printStackTrace();
							System.out.println("NAME_EXIST");
							httpRequests.personDelete(params);
							jsonResponse=httpRequests.personCreate(params);
    					}

            			Log.i(TAG,"person create response: "+jsonResponse);

            			jsonResponse=httpRequests.trainVerify(new PostParameters().setPersonName(personName));
            			
            			System.out.println(httpRequests.getSessionSync(jsonResponse.get("session_id").toString()));
            			
            			MainActivity.this.runOnUiThread(new Runnable(){
            				public void run() {
            					textView.setText("success.");
            				}
            			});
            		} catch (FaceppParseException e) {
            			e.printStackTrace();
            			MainActivity.this.runOnUiThread(new Runnable(){
            				public void run() {
            					textView.setText("Network error.");
            				}
            			});
            		} catch (JSONException e) {
            			e.printStackTrace();
						MainActivity.this.runOnUiThread(new Runnable(){
            				public void run() {
            					textView.setText("No face detected.");
            				}
            			});
            		}
    			}
    		}).start();
    	}
    }
    
    private class FaceppDetect {
    	Callback callback = null;
    	
    	public void setDetectCallback(Callback detectCallback) { 
    		callback = detectCallback;
    	}

    	public void detect(final Bitmap image) {
    		
    		new Thread(new Runnable() {
				
				public void run() {
		    		//Log.v(TAG, "image size : " + img.getWidth() + " " + img.getHeight());
		    		
		    		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		    		float scale = Math.min(1, Math.min(600f / image.getWidth(), 600f / image.getHeight()));
		    		Matrix matrix = new Matrix();
		    		matrix.postScale(scale, scale);

		    		Bitmap imgSmall = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, false);
		    		//Log.v(TAG, "imgSmall size : " + imgSmall.getWidth() + " " + imgSmall.getHeight());
		    		
		    		imgSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		    		byte[] array = stream.toByteArray();
		    		
		    		try {
		    			//detect
						jsonResponse = httpRequests.detectionDetect(new PostParameters().setImg(array));
						//finished , then call the callback function
						if (callback != null) {
							callback.getResult(jsonResponse, image);
						}
						tempId = jsonResponse.getJSONArray("face").getJSONObject(0).getString("face_id");
						faceId.add(tempId);
    					Log.i(TAG,"faceId = "+tempId);
					} catch (FaceppParseException e) {
						e.printStackTrace();
						MainActivity.this.runOnUiThread(new Runnable() {
							public void run() {
								textView.setText("Network error.");
							}
						});
					} catch (JSONException e) {
						e.printStackTrace();
						MainActivity.this.runOnUiThread(new Runnable() {
							public void run() {
								textView.setText("No face detected.");
							}
						});
						currentState = State.GET;
						buttonPhoto.setVisibility(View.VISIBLE);
						buttonGetImage.setVisibility(View.VISIBLE);
					}
					
				}
			}).start();
    	}
    }

    public class Callback {
    	public void getResult(JSONObject rst, Bitmap image) {
			//Log.i(TAG, rst.toString());
			
			//use the red paint
			Paint paint = new Paint();
			paint.setColor(Color.RED);
			paint.setStrokeWidth(Math.max(image.getWidth(), image.getHeight()) / 100f);

			//create a new canvas
			Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), image.getConfig());
			Canvas canvas = new Canvas(bitmap);
			canvas.drawBitmap(image, new Matrix(), null);
			
			
			try {
				//find out all faces
				final int count = rst.getJSONArray("face").length();
				for (int i = 0; i < count; ++i) {
					float x, y, w, h;
					//get the center point
					x = (float)rst.getJSONArray("face").getJSONObject(i)
							.getJSONObject("position").getJSONObject("center").getDouble("x");
					y = (float)rst.getJSONArray("face").getJSONObject(i)
							.getJSONObject("position").getJSONObject("center").getDouble("y");

					//get face size
					w = (float)rst.getJSONArray("face").getJSONObject(i)
							.getJSONObject("position").getDouble("width");
					h = (float)rst.getJSONArray("face").getJSONObject(i)
							.getJSONObject("position").getDouble("height");
					
					//change percent value to the real size
					x = x / 100 * image.getWidth();
					w = w / 100 * image.getWidth() * 0.7f;
					y = y / 100 * image.getHeight();
					h = h / 100 * image.getHeight() * 0.7f;

					//draw the box to mark it out
					canvas.drawLine(x - w, y - h, x - w, y + h, paint);
					canvas.drawLine(x - w, y - h, x + w, y - h, paint);
					canvas.drawLine(x + w, y + h, x - w, y + h, paint);
					canvas.drawLine(x + w, y + h, x + w, y - h, paint);
				}
				
				//save new image
				tempImg = bitmap;
				
				
				
				MainActivity.this.runOnUiThread(new Runnable() {
					
					public void run() {
						//show the image
						imageView.setImageBitmap(tempImg);
						textView.setText("Finished, "+ count + " faces.");
					}
				});
				
			} catch (JSONException e) {
				e.printStackTrace();
				MainActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						textView.setText("Error.");
					}
				});
			}
		}
	}
    
}
