#include <iostream> 

using namespace std;
using ll = long long;

const int N = 3e5;
const ll MOD = 1e9 + 9;
const ll BASE = 37;

ll rolling_hash (const string &s) {
    ll hash = 0;
    for (char c : s) {
        hash = (hash * BASE + (c - 'a' + 1)) % MOD;
    }
    return hash;
}

ll _pow(ll base, ll exp, ll mod);

ll imod (ll x) {
    return _pow(x, MOD-2, MOD);
}

ll _pow(ll base, ll exp, ll mod) {
    ll result = 1;
    while (exp > 0) {
        if (exp % 2 == 1) {
            result = (result * base) % mod;
        }
        base = (base * base) % mod;
        exp /= 2;
    }
    return result;
}

ll geom_series(int n, int len, ll base, ll mod) {
    return (_pow(BASE, n, MOD) - 1) * imod(_pow(BASE, len, MOD) - 1) % MOD;
}

int main () {

    string s; cin >> s;
    int n = (int)s.size();

    ll hash_s = rolling_hash(s);

    int rep_sz = -1;
   
    ll hash_pref = 0;
    for (int i = 0; i < n/2; i++) {
        hash_pref = (hash_pref * BASE + (s[i] - 'a' + 1)) % MOD;
        if (n % (i+1) == 0) {
            ll hash_full = (hash_pref * geom_series(n, i+1, BASE, MOD)) % MOD;
            if (hash_full == hash_s) {
                rep_sz = i+1;
                break;
            }
        }
    }

    if (rep_sz == -1) {
        cout << 1 << '\n';
        cout << s << endl;
    } else if (rep_sz == 1) {
        cout << n << '\n';
        for (char c:s) cout << c << ' ';
    }
    else {
        cout << 2 << '\n';
        cout << s[0] << ' ';
        cout << s.substr(1, s.size()-1) << endl;
    }


    return 0;
}