import java.io.*;
import java.net.DatagramPacket;
import java.util.*;
import java.nio.*;

class Packet {
    DatagramPacket pack;
    
    private int resendTime;         // The packet may have been resent for several times
    Packet(int datalength) {
        byte[] buf = new byte[24 + datalength];
        pack = new DatagramPacket(buf, buf.length);
        resendTime = 0;
    }

    Packet(DatagramPacket pack) {
        this.pack = pack;
        resendTime = 0;
    }

    public DatagramPacket getPacket() {
        return pack;
    }

    public int getResendTime() {
        return resendTime;
    }

    public int getSequencenumber() {
        ByteBuffer b = ByteBuffer.allocate(4);
        byte[] bytes = Arrays.copyOfRange(pack.getData(), 0, 4);
        return b.wrap(bytes).getInt();
    }
    public int getAckmber() {
        ByteBuffer b = ByteBuffer.allocate(4);
        byte[] bytes = Arrays.copyOfRange(pack.getData(), 4, 8);
        return b.wrap(bytes).getInt();      
    }
    public long getTimestamp() {
        ByteBuffer b = ByteBuffer.allocate(8);
        byte[] bytes = Arrays.copyOfRange(pack.getData(), 8, 16);
        return b.wrap(bytes).getLong();      
    }
    public int getLength() {
        ByteBuffer b = ByteBuffer.allocate(4);
        byte[] bytes = Arrays.copyOfRange(pack.getData(), 16, 20);
        int len = b.wrap(bytes).getInt();
        len = len >>> 3;
        return len;        
    }
    public boolean isACK() {
        byte[] buf = pack.getData();
        return (buf[19] & 1) == 1;
    }

    public boolean isFIN() {
        byte[] buf = pack.getData();
        return (buf[19] & 2) == 2;
    }

    public boolean isSYN() {
        byte[] buf = pack.getData();
        return (buf[19] & 4) == 4;
    }
    public int getChecksum() {
        int checkSum = 0;
        int count = 2;
        byte[] buf = pack.getData();
        InputStream ins = new ByteArrayInputStream(buf);
        try {
            ins.skip(22);
            while(count > 0) {
                count --;
                checkSum += ins.read() * (int)Math.pow(256, count);                
            } 
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                ins.close();
            } catch(IOException a) {
                a.printStackTrace();
            }
        }
        return checkSum;        
    }
    public int getData() {
        int data = 0;
        int count = 4;
        byte[] buf = pack.getData();
        InputStream ins = new ByteArrayInputStream(buf, 24, 4);
        try {
            ins.skip(24);
            while(count > 0) {
                count --;            
                data += ins.read() * (int)Math.pow(256, count);                
            } 
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                ins.close();
            } catch(IOException a) {
                a.printStackTrace();
            }
        }
        return data;        
    }

     // Set function for respective fields
    public void setSequencenumber(int seq) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(seq);
        byte[] result = b.array();
        byte[] buf = pack.getData();
        for (int i = 0; i < 4; i++) {
            buf[i] = result[i];
        }
    }

    public void setAcknumber(int ack) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(ack);
        byte[] result = b.array();
        byte[] buf = pack.getData();
        for (int i = 0; i < 4; i++) {
            buf[i + 4] = result[i];
        }
    }

    public void setTimestamp(long time) {
        ByteBuffer b = ByteBuffer.allocate(8);
        b.putLong(time);
        byte[] result = b.array();
        byte[] buf = pack.getData();
        for (int i = 0; i < 8; i++) {
            buf[i + 8] = result[i];
        }
    }

    public void setLength(int length) {
        ByteBuffer b = ByteBuffer.allocate(4);
        length = length << 3;
        b.putInt(length);
        byte[] result = b.array();
        byte[] buf = pack.getData();
        for (int i = 0; i < 3; i++) {
            buf[i + 16] = result[i];
        }
        buf[19] = (byte)(buf[19] & 0x07);
        buf[19] = (byte)(buf[19] | result[3]);
    }

    public void setSYN() {
        byte[] buf = pack.getData();
        buf[19] = (byte)(buf[19] | 0x04);
    }

    public void setACK() {
        byte[] buf = pack.getData();
        buf[19] = (byte)(buf[19] | 0x01);
    }

    public void setFIN() {
        byte[] buf = pack.getData();
        buf[19] = (byte)(buf[19] | 0x02);
    }
    // Should use the 2 part of sequence number to calculate this checksum field and make 1's complement
    public void setChecksum() {
        byte[] buf = pack.getData();
        ByteBuffer bb = ByteBuffer.wrap(buf);
        bb.rewind();
        int accumulation = 0;

        for (int i = 0; i < 2; ++i) {
            accumulation += 0xffff & bb.getShort();
        }
        // pad to an even number of shorts
        // if (buf.length % 2 > 0) {
        //     accumulation += (bb.get() & 0xff) << 8;
        // }

        accumulation = ((accumulation >> 16) & 0xffff)
                + (accumulation & 0xffff);
        short checksum = (short) (~accumulation & 0xffff);
        bb.putShort(22, checksum);
        byte[] result = bb.array();
    }

    public void setData(byte[] data) {
        byte[] buf = pack.getData();
        byte[] newBuf = new byte[data.length + 24];
        for (int i = 0; i < 24; i++) {
            newBuf[i] = buf[i];
        }
        for (int i = 0; i < data.length; i++) {
            buf[i + 24] = data[i];
        }
    }

    public void setResendTime(int resendTime) {
        this.resendTime = resendTime;
    }

    public static void main(String[] args) {
        Packet cur = new Packet(1000001);
        cur.setSequencenumber(5600);
        System.out.printf("sequence number is %d\n",cur.getSequencenumber());
        cur.setAcknumber(200);
        System.out.printf("Ack number is %d\n",cur.getAckmber());
        cur.setLength(1000001);
        System.out.printf("Data length is %d\n", cur.getLength());
        cur.setChecksum();
        System.out.printf("Checksum number is %d\n",cur.getChecksum());
        // cur.setACK();
        if(cur.isACK())
            System.out.printf("success set ACK\n");

        cur.setSYN();
        if(cur.isSYN())
            System.out.printf("success set SYN\n");

        cur.setFIN();
        if(cur.isFIN())
            System.out.printf("success set FIN\n");
        long lll = 356909657;
        cur.setTimestamp(lll);     
        System.out.printf("Time stamp is %d\n", cur.getTimestamp());
    }
}
