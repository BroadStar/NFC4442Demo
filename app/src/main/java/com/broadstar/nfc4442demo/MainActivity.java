package com.broadstar.nfc4442demo;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.broadstar.nfccardsdk.LogicCard;
import com.broadstar.nfccardsdk.NfcReader;
import com.broadstar.nfccardsdk.exception.APDUException;
import com.broadstar.nfccardsdk.exception.ReaderException;


public class MainActivity extends AppCompatActivity {

	// 卡片操作
	private LogicCard card;
	private IsoDep isodep; //ISO14443-4 NFC操作
	private NfcReader reader;

	// 界面控件
	private TextView dataView;
	private Button bt_read;
	private Button bt_clear;
	private EditText et_password;
	private Button bt_checkPW;
	private EditText et_data;
	private EditText et_address;
	private Button bt_write;
	private EditText protectedBlock;
	private Button readProtectedBlock;
	private Button writeProtectedBlock;
	private EditText secureBlock;
	private Button readSecureBlock;
	private EditText secureBlockAddress;
	private Button writeSecureBlock;
	private EditText protectedBlockAddress;
	private EditText et_writeProtectedBlock;

	//双击退出控制项
	private long exitTime = 0;

	// NFC相关
	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;
	public static String[][] TECHLISTS; //NFC技术列表
	public static IntentFilter[] FILTERS; //过滤器

	static {
		try {
			TECHLISTS = new String[][] { { IsoDep.class.getName() }, { NfcA.class.getName() } };

			FILTERS = new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED, "*/*") };
		} catch (Exception ignored) {
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		nfcAdapter = NfcAdapter.getDefaultAdapter(this);

		dataView = (TextView) findViewById(R.id.dataView);
		bt_read = (Button) findViewById(R.id.bt_read);
		bt_clear = (Button) findViewById(R.id.bt_clear);
		et_password = (EditText) findViewById(R.id.et_password);
		bt_checkPW = (Button) findViewById(R.id.bt_checkPW);
		et_data = (EditText) findViewById(R.id.et_data);
		et_address = (EditText) findViewById(R.id.et_address);
		bt_write = (Button) findViewById(R.id.bt_write);

		protectedBlock = (EditText) findViewById(R.id.protectedBlock);
		readProtectedBlock = (Button) findViewById(R.id.readProtectedBlock);
		writeProtectedBlock = (Button) findViewById(R.id.writeProtectedBlock);

		secureBlock = (EditText) findViewById(R.id.secureBlock);
		readSecureBlock = (Button) findViewById(R.id.readSecureBlock);
		secureBlockAddress = (EditText) findViewById(R.id.secureBlockAddress);

		writeSecureBlock = (Button) findViewById(R.id.writeSecureBlock);
		protectedBlockAddress = (EditText) findViewById(R.id.protectedBlockAddress);
		et_writeProtectedBlock = (EditText) findViewById(R.id.et_writeProtectedBlock);

		et_address.addTextChangedListener(new HexTextWatcher(et_address));
		et_data.addTextChangedListener(new HexTextWatcher(et_data));
		et_password.addTextChangedListener(new HexTextWatcher(et_password));
		et_writeProtectedBlock.addTextChangedListener(new HexTextWatcher(et_writeProtectedBlock));
		protectedBlock.addTextChangedListener(new HexTextWatcher(protectedBlock));
		secureBlock.addTextChangedListener(new HexTextWatcher(secureBlock));
		protectedBlockAddress.addTextChangedListener(new HexTextWatcher(protectedBlockAddress));
		secureBlockAddress.addTextChangedListener(new HexTextWatcher(secureBlockAddress));

		bt_read.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				readData();
			}
		});

		bt_clear.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dataView.setText("");
			}
		});

		bt_checkPW.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isodep == null)
					return;
				String password = et_password.getText().toString().replaceAll(" ", "");
				if(password.length() != 6) {
					displayToast("密码长度错误");
					return;
				}
				try {
					card.checkPW(hexStringToBytes(password));
					displayToast("密码校验成功");
				} catch (ReaderException e) {
					e.printStackTrace();
					displayToast("密码校验失败: 连接错误");
				} catch (APDUException e) {
					e.printStackTrace();
					String sw = e.getResponse();
					if (sw.substring(0,2).equals("63")) {
						displayToast("密码校验失败: 剩余次数" + sw.charAt(3));
					} else {
						displayToast("密码校验失败: " + e.getMessage());
					}
				}
			}
		});

		bt_write.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isodep == null)
					return;
				String address = et_address.getText().toString().replaceAll(" ", "");
				int add;
				if (address.length() != 0) {
					add = Integer.parseInt(address, 16);
					if (!(add <= 255 && add >= 0)) {
						displayToast("地址输入错误");
						return;
					}
				} else {
					displayToast("地址输入错误");
					return;
				}
				String data = et_data.getText().toString().replaceAll(" ", "");
				if (data.length() == 0 || data.length() % 2 != 0) {
					displayToast("数据长度错误");
					return;
				}
				if (data.length() / 2 + add > LogicCard.MAX_LENGTH) {
					displayToast("地址或数据长度错误");
					return;
				}
				try {
					card.writeBlock(add, data.length() / 2, hexStringToBytes(data));
					displayToast("数据写入成功");
				} catch (ReaderException | APDUException e) {
					e.printStackTrace();
					displayToast("数据写入失败: " + e.getMessage());
				}
			}
		});

		readSecureBlock.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isodep == null) {
					return;
				}
				try {
					byte[] result = card.readSecureBlock();
					secureBlock.setText(bytesToHexString(result));
					secureBlockAddress.setText("00");
					displayToast("读取成功");
				} catch (APDUException | ReaderException e) {
					e.printStackTrace();
					displayToast(e.getMessage());
				}
			}
		});

		writeSecureBlock.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isodep == null)
					return;
				String str = secureBlock.getText().toString().replaceAll(" ", "");
				String address = secureBlockAddress.getText().toString().replaceAll(" ", "");
				if (!str.equals("")) {
					byte[] data = hexStringToBytes(str);
					try {
						int add = Integer.parseInt(address, 16);
						card.writeSecureBlock(add, data.length, data);
						displayToast("写入成功");
					} catch (APDUException | ReaderException e) {
						e.printStackTrace();
						displayToast(e.getMessage());
					}
				}
			}
		});

		readProtectedBlock.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isodep == null)
					return;
				try {
					byte[] result = card.readProtectedBlock();
					protectedBlock.setText(bytesToHexString(result));
					displayToast("读取成功");
				} catch (APDUException | ReaderException e) {
					e.printStackTrace();
					displayToast(e.getMessage());
				}
			}
		});

		writeProtectedBlock.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isodep == null)
					return;
				String str = et_writeProtectedBlock.getText().toString().replaceAll(" ", "");
				String address = protectedBlockAddress.getText().toString().replaceAll(" ", "");
				if (!str.equals("")) {
					byte[] data = hexStringToBytes(str);
					try {
						int add = Integer.parseInt(address, 16);
						card.writeProtectedBlock(add, data.length, data);
						displayToast("写入成功");
					} catch (APDUException | ReaderException e) {
						e.printStackTrace();
						displayToast(e.getMessage());
					}
				}
			}
		});

		// 初始化卡片信息

		pendingIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		onNewIntent(getIntent());
	}

	//菜单项创建事件
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	//菜单项点击事件
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
			case R.id.action_about:
				showVersionInfo();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	//刷新NFC状态
	private void refreshStatus() {
		String tip;
		if (nfcAdapter == null) {
			tip = "没有NFC硬件";
		} else if (!nfcAdapter.isEnabled()) {
			tip = "NFC已禁用";
		} else {
			setTitle(getResources().getString(R.string.app_name));
			return;
		}
		final StringBuilder s = new StringBuilder(getResources().getString(R.string.app_name));
		s.append("  --  ").append(tip);
		setTitle(s);
	}

	//显示版本信息
	private void showVersionInfo() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		PackageInfo packageInfo = null;
		String version = null;

		try {
            /* Get the version name. */
			packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = "Version " + packageInfo.versionName;
		} catch (NameNotFoundException e) {
			version = "Unknown Version";
		}

		builder.setMessage(version)
				.setTitle("关于")
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		builder.show();
	}

	private void readData() {
		if (isodep == null) {
			displayToast("请重新贴卡");
			return;
		}
		try {
			byte[] data = readAllBlock();
			Log.d("数据长度", "" + data.length);
//			protectedBlockAddress.setText("00");
//			et_writeProtectedBlock.setText(Util.bytesToHexString(data).substring(0, 64));
			dataView.setText(dealData(data));
		} catch (ReaderException e) {
			e.printStackTrace();
			displayToast("连接错误");
		} catch (APDUException e) {
			e.printStackTrace();
			displayToast("读取失败");
		}

	}

	String dealData(byte[] data) {
		String stringData = bytesToHexString(data);
		String result = stringData.substring(0, 32);
		for(int i=1; i<16; i++) {
				result = result + "\n" + stringData.substring(i * 32, (i + 1) * 32);
		}
		return result;
	}

	//显示提示信息
	public void displayToast(String str) {
		Toast toast=Toast.makeText(this, str, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.TOP,0,220);
		toast.show();
	}

	//处理NFC触发
	@Override
	protected void onNewIntent(Intent intent) {
		//从intent中获取标签信息
		Parcelable p = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		if (p != null) {
			Tag tag = (Tag) p;
			isodep = IsoDep.get(tag);
			if (isodep != null){
				if (reader == null) {
					reader = new NfcReader(isodep);
				}
				else
					reader.setIsoDep(isodep);
				try {
					reader.reset();
				} catch (ReaderException e) {
					e.printStackTrace();
					return;
				}
				if (card == null)
					card = new LogicCard(reader);
				readData();
			}
		}
	}

	//程序恢复
	@Override
	protected void onResume() {
		super.onResume();
		refreshStatus();
		if (nfcAdapter != null) {
			nfcAdapter.enableForegroundDispatch(this, pendingIntent, FILTERS, TECHLISTS);
		}
	}

	//程序暂停
	@Override
	protected void onPause() {
		super.onPause();
		if (nfcAdapter != null)
			nfcAdapter.disableForegroundDispatch(this);
	}

	//双击退出
	@Override
	public void onBackPressed() {
		if ((System.currentTimeMillis() - exitTime) > 2000) {
			Toast.makeText(this, "再次按返回键退出", Toast.LENGTH_SHORT).show();
			exitTime = System.currentTimeMillis();
		} else {
			super.onBackPressed();
		}
	}

	public class HexTextWatcher implements TextWatcher {

		private static final String TAG = "HexTextWatcher";

		private boolean mFormat;
		private boolean mInvalid;
		private int mSelection;
		private String mLastText;

		/**
		 * The editText to edit text.
		 */
		private EditText mEditText;

		/**
		 * Creates an instance of <code>HexTextWatcher</code>.
		 *
		 * @param editText
		 *        the editText to edit text.
		 */
		public HexTextWatcher(EditText editText) {
			mFormat = false;
			mInvalid = false;
			mLastText = "";
			this.mEditText = editText;
		}

		@Override
		public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			try {
				String temp = s.toString();
				// Set selection.
				if (mLastText.equals(temp)) {
					if (mInvalid) {
						mSelection -= 1;
					} else {
						if ((mSelection >= 1) && (temp.length() > mSelection - 1)
								&& (temp.charAt(mSelection - 1)) == ' ') {
							mSelection += 1;
						}
					}
					int length = mLastText.length();
					if (mSelection > length) {
						mEditText.setSelection(length);
					} else {
						mEditText.setSelection(mSelection);
					}
					mFormat = false;
					mInvalid = false;
					return;
				}

				mFormat = true;
				mSelection = start;

				// Delete operation.
				if (count == 0) {
					if ((mSelection >= 1) && (temp.length() > mSelection - 1)
							&& (temp.charAt(mSelection - 1)) == ' ') {
						mSelection -= 1;
					}
					return;
				}

				// Input operation.
				mSelection += count;
				char[] lastChar = (temp.substring(start, start + count))
						.toCharArray();
				int mid = lastChar[0];
				if (mid >= 48 && mid <= 57) {
                /* 1-9. */
				} else if (mid >= 65 && mid <= 70) {
                /* A-F. */
				} else if (mid >= 97 && mid <= 102) {
                /* a-f. */
				} else {
                /* Invalid input. */
					mInvalid = true;
					temp = temp.substring(0, start)
							+ temp.substring(start + count, temp.length());
					mEditText.setText(temp);
				}
			} catch (Exception e) {
				Log.i(TAG, e.toString());
			}
		}

		@Override
		public void afterTextChanged(Editable s) {
			try {
            /* Format input. */
				if (mFormat) {
					StringBuilder text = new StringBuilder();
					text.append(s.toString().replace(" ", ""));
					int length = text.length();
					int sum = (length % 2 == 0) ? (length / 2) - 1 : (length / 2);
					for (int offset = 2, index = 0; index < sum; offset += 3, index++) {
						text.insert(offset, " ");
					}
					mLastText = text.toString().toUpperCase();
					mEditText.setText(text.toString().toUpperCase());
				}
			} catch (Exception e) {
				Log.i(TAG, e.toString());
			}
		}
	}

	/**
	 * 读取逻辑卡主存储区全部数据
	 * @return 数据
	 * @throws ReaderException
	 * @throws APDUException
	 */
	public synchronized byte[] readAllBlock() throws ReaderException, APDUException {
		//分两次读取，每次读取一半
		byte[] result = new byte[LogicCard.MAX_LENGTH];
		byte[] result1 = card.readBlock(0, LogicCard.MAX_LENGTH / 2);
		byte[] result2 = card.readBlock(LogicCard.MAX_LENGTH / 2, LogicCard.MAX_LENGTH / 2);
		System.arraycopy(result1, 0, result, 0, result1.length);
		System.arraycopy(result2, 0, result, LogicCard.MAX_LENGTH / 2, result2.length);
		return result;
	}

	public static byte[] hexStringToBytes(String hexString) {
		byte digest[] = new byte[hexString.length() / 2];
		for (int i = 0; i < digest.length; i++) {
			String byteString = hexString.substring(2 * i, 2 * i + 2);
			int byteValue = Integer.parseInt(byteString, 16);
			digest[i] = (byte) byteValue;
		}
		return digest;
	}

	public static String bytesToHexString(byte[] bArray) {
		StringBuffer sb = new StringBuffer(bArray.length);
		String sTemp;
		for (byte aBArray : bArray) {
			sTemp = Integer.toHexString(0xFF & aBArray);
			if (sTemp.length() < 2)
				sb.append(0);
			sb.append(sTemp.toUpperCase());
		}
		return sb.toString();
	}


}
