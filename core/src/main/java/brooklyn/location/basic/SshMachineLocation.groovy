package brooklyn.location.basic

import brooklyn.location.MachineLocation
import brooklyn.location.PortRange
import brooklyn.util.internal.SshJschTool

/**
 * Operations on a machine that is accessible via ssh.
 */
public class SshMachineLocation extends AbstractLocation implements MachineLocation {
    private String user = null
    private InetAddress address
    private final List<Integer> portsInUse = []
    
    public SshMachineLocation(InetAddress address, String userName = null, String name = null) {
        super(name ?: address.getHostName(), null)
        this.user = userName
        this.address = address
    }

    public InetAddress getAddress() {
        return this.address;
    }

    /** convenience for running a script, returning the result code */
    public int run(Map props=[:], String command) {
        assert address : "host must be specified for $this"

        if (!user) user=System.getProperty "user.name"
        def t = new SshJschTool(user:user, host:address.hostName);
        t.connect()
        int result = t.execShell props, command
        t.disconnect()
        result

//        ExecUtils.execBlocking "ssh", (user?user+"@":"")+host, command
    }

    public int copyTo(File src, String destination) {
        def conn = new SshJschTool(user:user, host:address.hostName)
        conn.connect()
        int result = conn.copyToServer [:], src, destination
        conn.disconnect()
        result
    }

    @Override
    public String toString() {
        return address;
    }

    // TODO Does not support zero to mean any; but can't when returning boolean
    // TODO Does not yet check if the port really is free on this machine
    boolean obtainSpecificPort(int portNumber) {
        if (portsInUse.contains(portNumber)) {
            return false
        } else {
            portsInUse.add(portNumber)
            return true
        }
    }

    int obtainPort(PortRange range) {
        for (int i = range.getMin(); i <= range.getMax(); i++) {
            if (obtainSpecificPort(i)) return i;
        }
        return -1;
    }

    void releasePort(int portNumber) {
        portsInUse.remove((Object)portNumber);
    }
}
