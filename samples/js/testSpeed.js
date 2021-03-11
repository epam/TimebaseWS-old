'use strict';

const runTest = () => {
    let currentTest = 1;
    let testCount = 1;
    let messages = 0;
    let bytes = 0;

    function nextTest() {
        let ws;
        let ts0 = 0;
        let ts1 = 0;

        if (currentTest > testCount) {
            return;
        }

        console.log(`Running test case ${currentTest}/${testCount}`);

        ws = new WebSocket(`ws://localhost:8099/ws/v0/test.stream/select?live=true`);
        ws.onmessage = (data) => {
            if (messages === 0) {
                ts0 = new Date().getTime();
            }
            console.log(`got message: ${data}`);
            messages++;
            bytes += data.length;
            if (messages === 10000) {
                ws.close()
            }
        };
        ws.onclose = () => {
            currentTest++;
            console.log(`got messages: ${messages}, bytes: ${bytes}`);
            ts1 = new Date().getTime();
            const s = (ts1 - ts0) * 0.001;
            console.log(`${messages} msg ${s} sec; speed: ${messages / s} msg/s, ${bytes / (1024 * 1024) / s} MB/s`);
            messages = 0;
            bytes = 0;
        };
        ws.onerror = (e) => console.error(e);
    }

    nextTest();
};
runTest();
