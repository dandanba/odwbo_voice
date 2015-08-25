package kr.re.Dev.ArduinoEcho;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Set;
import kr.re.Dev.Bluetooth.BluetoothSerialClient;
import kr.re.Dev.Bluetooth.BluetoothSerialClient.BluetoothStreamingHandler;
import kr.re.Dev.Bluetooth.BluetoothSerialClient.OnBluetoothEnabledListener;
import kr.re.Dev.Bluetooth.BluetoothSerialClient.OnScanListener;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.odwbo.voice.R;

/**
 * Blutooth Arduino Echo.
 * 
 * 문자열의 끝은 '\0' 을 붙여서 구분한다.
 * 
 * www.dev.re.kr
 * 
 * @author ice3x2@gmail.com / Beom
 */
public class BTManager {
	public static final String TAG = BTManager.class.getSimpleName();
	public final LinkedList<BluetoothDevice> mBluetoothDevices = new LinkedList<BluetoothDevice>();
	public final BluetoothStreamingHandler mBTHandler = new BluetoothStreamingHandler() {
		ByteBuffer mByteBuffer = ByteBuffer.allocate(1024);

		@Override
		public void onError(Exception e) {
			Log.i(TAG, "Messgae : Connection error - " + e.toString() + "\n");
		}

		@Override
		public void onDisconnected() {
			Log.i(TAG, "Messgae : Disconnected.\n");
		}

		@Override
		public void onData(byte[] buffer, int length) {
			if (length == 0) {
				return;
			}
			synchronized (mByteBuffer) {
				mByteBuffer.put(buffer, 0, length);
				final int size = mByteBuffer.position();
				if (size >= 0X0A) {
					buffer = mByteBuffer.array();
					mByteBuffer.clear();
					if (size > 0X0A) {
						mByteBuffer.put(buffer, 0X0A, size - 0X0A);
					}
					// // 88 BB LEN V IR
					if (buffer[0] == ((byte) 0X88) && buffer[1] == ((byte) 0XBB) && buffer[2] == 0x0A) {
						// Bit 0 IR1 红外1状态 0-无障碍，1-有障碍
						// Bit 1 IR2 红外2状态 0-无障碍，1-有障碍
						// Bit 2 IR3 红外3状态 0-无障碍，1-有障碍
						// Bit 3 IR4 红外4状态 0-无障碍，1-有障碍
						// Bit 4 00 保留 00
						// Bit 5 00 保留 00
						// Bit 6 00 保留 00
						// Bit 7 En 自主避障使能 0-关闭，1-开启
						final byte V = buffer[3];
						final byte IR = buffer[4];
						final byte[] Bit = getBooleanArray(IR);
						final boolean IR1 = Bit[0] == 1;
						final boolean IR2 = Bit[1] == 1;
						final boolean IR3 = Bit[2] == 1;
						final boolean IR4 = Bit[3] == 1;
						final byte DH = buffer[5];
						final byte DL = buffer[6];
						int Voltage = V > 125 ? 125 : V < 110 ? 110 : V;
						Voltage = (Voltage - 110) * 10 / 16;
						int Distance = (DH << 8) + DL;
						String data = "receive:" + (IR1 + ":" + IR2 + ":" + IR3 + ":" + IR4) + ":" + Voltage + ":" + Distance;
						// receive:IR1:IR2:IR3:IR4:Voltage:Distance
						receiveStringData(data);
					}
				}
			}
		}

		@Override
		public void onConnected() {
			Log.i(TAG, "Messgae : Connected. " + mClient.getConnectedDevice().getName() + "\n");
		}
	};
	public ArrayAdapter<String> mDeviceArrayAdapter;
	public AlertDialog mDeviceListDialog;
	public BluetoothSerialClient mClient;
	private Context mContext;

	public byte[] getBooleanArray(byte b) {
		byte[] array = new byte[8];
		for (int i = 0; i < 7; i++) {
			array[i] = (byte) (b & 1);
			b = (byte) (b >> 1);
		}
		return array;
	}

	public boolean create(Context context) {
		mClient = BluetoothSerialClient.getInstance();
		if (mClient == null) {
			Toast.makeText(context, "Cannot use the Bluetooth device.", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	public void pause(Context context) {
		cancelScan(context);
	}

	public void resume(Context context) {
		enableBluetooth(context);
	}

	public void destroy() {
		mClient.claer();
	}

	public void addDeviceToArrayAdapter(BluetoothDevice device) {
		if (mBluetoothDevices.contains(device)) {
			mBluetoothDevices.remove(device);
			mDeviceArrayAdapter.remove(device.getName() + "\n" + device.getAddress());
		}
		mBluetoothDevices.add(device);
		mDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress());
		mDeviceArrayAdapter.notifyDataSetChanged();
		if (device.getName().equals("OmmiBot")) {
			connect(mContext, device);
			mDeviceListDialog.cancel();
		}
	} 

	public void enableBluetooth(Context context) {
		BluetoothSerialClient btSet = mClient;
		btSet.enableBluetooth(context, new OnBluetoothEnabledListener() {
			@Override
			public void onBluetoothEnabled(boolean success) {
				if (success) {
					getPairedDevices();
				}
			}
		});
	}

	public void getPairedDevices() {
		Set<BluetoothDevice> devices = mClient.getPairedDevices();
		for (BluetoothDevice device : devices) {
			addDeviceToArrayAdapter(device);
		}
	}

	public void scanDevices(Context context) {
		BluetoothSerialClient btSet = mClient;
		btSet.scanDevices(context, new OnScanListener() {
			@Override
			public void onStart() {
				Log.i(TAG, "Scanning....");
			}

			@Override
			public void onFoundDevice(BluetoothDevice bluetoothDevice) {
				Log.i(TAG, "Found " + bluetoothDevice.getName() + " " + bluetoothDevice.getAddress());
				addDeviceToArrayAdapter(bluetoothDevice);
			}

			@Override
			public void onFinish() {
				Log.i(TAG, "Scan finish.");
				mDeviceListDialog.show();
			}
		});
	}

	public void cancelScan(Context context) {
		BluetoothSerialClient btSet = mClient;
		btSet.cancelScan(context);
	}

	public void connect(Context context, BluetoothDevice device) {
		BluetoothSerialClient btSet = mClient;
		btSet.connect(context, device, mBTHandler);
	}

	public void initDeviceListDialog(Context context) {
		mContext = context;
		mDeviceArrayAdapter = new ArrayAdapter<String>(context, R.layout.item_device);
		ListView listView = new ListView(context);
		listView.setAdapter(mDeviceArrayAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String item = (String) parent.getItemAtPosition(position);
				for (BluetoothDevice device : mBluetoothDevices) {
					if (item.contains(device.getAddress())) {
						connect(mContext, device);
						mDeviceListDialog.cancel();
					}
				}
			}
		});
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Select bluetooth device");
		builder.setView(listView);
		builder.setPositiveButton("Scan", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				scanDevices(mContext);
			}
		});
		mDeviceListDialog = builder.create();
		mDeviceListDialog.setCanceledOnTouchOutside(false);
	}

	public void showDeviceListDialog() {
		mDeviceListDialog.show();
	}

	/**
	 * @param data
	 */
	public void receiveStringData(String data) {
		Log.i(TAG, "receiveStringData<<<<<<<<<<<<<<-" + data);
	}

	public void sendStringData(String data) {
		// （2）机器人直线运动：
		// 帧长度：帧内所有数据字节数
		// 设备ID：11
		// 参数：运动方向和运动速度
		// 校验：所有数据的求和取低字节
		// 66 AA 0B 11 DH DL VH VL 00 00 CheckSum
		// Direction = (DH<<8 + DL)，范围0-3599，单位0.1°
		// Speed = (VH<<8 + VL)，范围0-250，单位mm/s
		Log.i(TAG, "sendStringData->>>>>>>>>" + data);
		// onSendTextMesage("touch:" + direction + ":" + speed);
		if (data.startsWith("touch:")) {
			final String[] sa = data.split(":");
			final int Direction = Integer.parseInt(sa[1]);
			final int Speed = Integer.parseInt(sa[2]);
			final byte DH = (byte) (Direction >> 8);
			final byte DL = (byte) Direction;
			final byte VH = (byte) (Speed >> 8);
			final byte VL = (byte) Speed;
			final byte CheckSum = (byte) (0X66 + (byte) 0xAA + 0X0B + 0X11 + DH + DL + VH + VL);
			byte[] buffer = new byte[] { 0X66, (byte) 0xAA, 0X0B, 0X11, DH, DL, VH, VL, 0X00, 0X00, CheckSum };
			if (mBTHandler.write(buffer)) {
				Log.i(TAG, "touch->>>>>>>>>" + data);
			}
		}
		// （4）三轮独立控制：
		// 帧头：66 AA
		// 帧长度：帧内所有数据字节数
		// 设备ID：13
		// 参数：三个轮子运动速度
		// 校验：所有数据的求和取低字节
		// 66 AA 0B 13 V1H V1L V2H V2L V3H V3L CheckSum
		// V = (VH<<8 + VL)，带符号16位，范围±250，单位mm/s
		// onSendTextMesage("speed:" + speed);
		else if (data.startsWith("speed:")) {
			final int Speed = Integer.parseInt(data.split(":")[1]);
			byte VH = (byte) (Speed >> 8);
			byte VL = (byte) Speed;
			byte CheckSum = (byte) (0X66 + (byte) 0xAA + 0X0B + 0X13 + VH + VL + VH + VL + VH + VL);
			byte[] buffer = new byte[] { 0X66, (byte) 0xAA, 0X0B, 0X13, VH, VL, VH, VL, VH, VL, CheckSum };
			if (mBTHandler.write(buffer)) {
				Log.i(TAG, "speed->>>>>>>>>" + data);
			}
		}
	}
}
