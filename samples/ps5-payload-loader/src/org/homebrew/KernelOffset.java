package org.homebrew;

public class KernelOffset {

    public static final long ALLPROC;
    public static final long PRISON0;
    public static final long ROOTVNODE;
    public static final long SECURITY_FLAGS;
    public static final long QA_FLAGS;
    public static final long UTOKEN_FLAGS;
    public static final long TARGETID;

    public static final int PROC_UCRED  = 64;
    public static final int PROC_FD     = 72;
    public static final int PROC_PID    = 188;

    static {
	switch(libkernel.getSystemSwVersion() & 0xffff0000) {
	case 0x3000000:
	case 0x3100000:
	case 0x3200000:
	case 0x3210000:
	    ALLPROC        = 0x276DC58;
	    SECURITY_FLAGS = 0x6466474;
	    TARGETID       = 0x646647D;
	    QA_FLAGS       = 0x6466498;
	    UTOKEN_FLAGS   = 0x6466500;
	    PRISON0        = 0x1CC2670;
	    ROOTVNODE      = 0x67AB4C0;
	    break;

	case 0x4020000:
	    ALLPROC        = 0x27EDCB8;
	    SECURITY_FLAGS = 0x6505474;
	    TARGETID       = 0x650547D;
	    QA_FLAGS       = 0x6505498;
	    UTOKEN_FLAGS   = 0x6505500;
	    PRISON0        = 0x1D34D00;
	    ROOTVNODE      = 0x66E64C0;
	    break;

	case 0x4000000:
	case 0x4030000:
	case 0x4500000:
	case 0x4510000:
	    ALLPROC        = 0x27EDCB8;
	    SECURITY_FLAGS = 0x6506474;
	    TARGETID       = 0x650647D;
	    QA_FLAGS       = 0x6506498;
	    UTOKEN_FLAGS   = 0x6506500;
	    PRISON0        = 0x1D34D00;
	    ROOTVNODE      = 0x66E74C0;
	    break;
			
        case 0x5000000:
        case 0x5020000:
        case 0x5100000:
        case 0x5500000:
	    ALLPROC        = 0x290DD00;
	    SECURITY_FLAGS = 0x66366EC;
	    TARGETID       = 0x66366F5;
	    QA_FLAGS       = 0x6636710;
	    UTOKEN_FLAGS   = 0x6636778;
	    PRISON0        = 0x1E119C0;
	    ROOTVNODE      = 0x6843510;
	    break;

        case 0x6000000:
        case 0x6020000:
        case 0x6500000:
	    ALLPROC        = 0x2859D20;
	    SECURITY_FLAGS = 0x65868EC;
	    TARGETID       = 0x65868F5;
	    QA_FLAGS       = 0x6586910;
	    UTOKEN_FLAGS   = 0x6586978;
	    PRISON0        = 0x1E35560;
	    ROOTVNODE      = 0x678F510;
	    break;

        case 0x7000000:
        case 0x7010000:
        case 0x7200000:
        case 0x7400000:
        case 0x7600000:
        case 0x7610000:
	    ALLPROC        = 0x2849D50;
	    SECURITY_FLAGS = 0x0AB8064;
	    TARGETID       = 0x0AB806D;
	    QA_FLAGS       = 0x0AB8088;
	    UTOKEN_FLAGS   = 0x0AB80F0;
	    PRISON0        = 0x1E35C00;
	    ROOTVNODE      = 0x30B7510;
	    break;

	default:
	    ALLPROC        = 0;
	    SECURITY_FLAGS = 0;
	    TARGETID       = 0;
	    QA_FLAGS       = 0;
	    UTOKEN_FLAGS   = 0;
	    PRISON0        = 0;
	    ROOTVNODE      = 0;
	}
    }
}
