/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/zkhan/git/avare/app/src/main/aidl/com/ds/avare/IHelper.aidl
 */
package com.ds.avare;
public interface IHelper extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.ds.avare.IHelper
{
private static final java.lang.String DESCRIPTOR = "com.ds.avare.IHelper";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.ds.avare.IHelper interface,
 * generating a proxy if needed.
 */
public static com.ds.avare.IHelper asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.ds.avare.IHelper))) {
return ((com.ds.avare.IHelper)iin);
}
return new com.ds.avare.IHelper.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_sendDataText:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.sendDataText(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_recvDataText:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.recvDataText();
reply.writeNoException();
reply.writeString(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.ds.avare.IHelper
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void sendDataText(java.lang.String text) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(text);
mRemote.transact(Stub.TRANSACTION_sendDataText, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.lang.String recvDataText() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_recvDataText, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_sendDataText = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_recvDataText = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public void sendDataText(java.lang.String text) throws android.os.RemoteException;
public java.lang.String recvDataText() throws android.os.RemoteException;
}
